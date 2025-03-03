package com.aolda.itda.service;

import com.aolda.itda.dto.auth.LoginRequestDTO;
import com.aolda.itda.dto.auth.LoginResponseDTO;
import com.aolda.itda.dto.auth.ProjectIdAndNameDTO;
import com.aolda.itda.dto.auth.ProjectRoleDTO;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${spring.server.keystone}")
    private String keystone;
    @Value("${spring.server.admin-id}")
    private String adminId;
    @Value("${spring.server.admin-password}")
    private String adminPassword;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 사용자 로그인 후 토큰 발행 및 Role 반환
    public LoginResponseDTO userLogin(HttpServletResponse response, LoginRequestDTO loginRequestDTO) throws JsonProcessingException {
        Map<String, String> user = getToken(loginRequestDTO.getId(), loginRequestDTO.getPassword());

        String userId = user.get("id");
        String token = user.get("token");

        if (userId == null || token == null) {
            throw new CustomException(ErrorCode.INVALID_USER_INFO);
        }

        response.addHeader("X-Subject-Token", token);
        return LoginResponseDTO.builder()
                .isAdmin(false)
                .lists(getProjectsWithUser(user))
                .build();
    }

    // 특정 사용자의 토큰 발행
    private Map<String, String> getToken(String id, String password) {

        String url = keystone + "/auth/tokens";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\n" +
                "    \"auth\": {\n" +
                "        \"identity\": {\n" +
                "            \"methods\": [\n" +
                "                \"password\"\n" +
                "            ],\n" +
                "            \"password\": {\n" +
                "                \"user\": {\n" +
                "                    \"name\": \""+ id + "\",\n" +
                "                    \"domain\": {\n" +
                "                        \"name\": \"Default\"\n" +
                "                    },\n" +
                "                    \"password\": \"" + password + "\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> res = restTemplate.postForEntity(url, requestEntity, Map.class);

        Map<String, Object> resToken = (Map<String, Object>) res.getBody().get("token");
        Map<String, Object> resUser = (Map<String, Object>) resToken.get("user");
        String userId = (String) resUser.get("id");
        String token = res.getHeaders().getFirst("X-Subject-Token");

        return Map.of("id", userId,
                    "token", token);
    }

    // 특정 사용자의 특정 프로젝트 내 최고 권한 반환
    public Map<String, String> getBestRoleWithinProject(String token, String projectId) throws JsonProcessingException {
        return getBestRoleWithinProject(Map.of(
                "id", validateTokenAndGetUserId(token),
                "token", token),
                projectId);
    }

    private Map<String, String> getBestRoleWithinProject(Map<String, String> user, String projectId) throws JsonProcessingException {
        String userId = user.get("id");
        String token = user.get("token");

        if (userId == null || token == null) {
            throw new CustomException(ErrorCode.INVALID_USER_INFO);
        }

        String url = keystone + "/role_assignments?user.id=" + userId + "&effective&include_names=true&scope.project.id=" + projectId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", getAdminToken());

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        JsonNode node = objectMapper.readTree(res.getBody());
        ArrayNode arrayNode = (ArrayNode) node.get("role_assignments");

        String bestRole = "reader";

        for (JsonNode assignment : arrayNode) {

            String roleName = assignment.path("role").path("name").asText();

            if (roleName.equals("admin")) { // admin인 경우
                bestRole = roleName;
            } else if (roleName.equals("manager") && !bestRole.equals("admin")) { // 최고 권한이 admin이 아닌 경우
                bestRole = roleName;
            } else if (roleName.equals("member") && bestRole.equals("reader")) { // 최고 권한이 reader인 경우
                bestRole = roleName;
            }

        }

        return Map.of("role", bestRole);
    }

    // 관리자용 토큰 발행
    public String getAdminToken() {
        Map<String, String> user = getToken(adminId, adminPassword);
        return user.get("token");
    }

    // 특정 사용자의 참여 프로젝트 반환
    private List<ProjectIdAndNameDTO> getProjectsWithUser(Map<String, String> user) throws JsonProcessingException {
        String userId = user.get("id");
        String token = user.get("token");
        if (userId == null || token == null) {
            throw new CustomException(ErrorCode.INVALID_USER_INFO);
        }

        String url = keystone + "/users/" + userId + "/projects";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", getAdminToken());

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        JsonNode node = objectMapper.readTree(res.getBody());
        ArrayNode arrayNode = (ArrayNode) node.get("projects");

        List<ProjectIdAndNameDTO> lists = new ArrayList<>();

        for (JsonNode assignment : arrayNode) {
            String projectId = assignment.path("id").asText();
            String projectName = assignment.path("name").asText();
            lists.add(new ProjectIdAndNameDTO(projectId, projectName));
        }
        return lists;
    }

    private String validateTokenAndGetUserId(String token) throws JsonProcessingException {
        String url = keystone + "/auth/tokens";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", getAdminToken());
        headers.set("X-Subject-Token", token);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> res;
        try {
            res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return objectMapper.readTree(res.getBody()).path("token").path("user").path("id").asText();

    }
}
