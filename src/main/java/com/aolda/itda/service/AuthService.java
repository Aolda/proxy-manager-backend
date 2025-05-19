package com.aolda.itda.service;

import com.aolda.itda.dto.auth.LoginRequestDTO;
import com.aolda.itda.dto.auth.LoginResponseDTO;
import com.aolda.itda.dto.auth.IdAndNameDTO;
import com.aolda.itda.entity.user.User;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.user.UserRepository;
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
    private final UserRepository userRepository;

    // 사용자 로그인 후 토큰 발행 및 Role 반환
    public LoginResponseDTO userLogin(HttpServletResponse response, LoginRequestDTO loginRequestDTO) throws JsonProcessingException {
        Map<String, String> user = getToken(loginRequestDTO.getId(), loginRequestDTO.getPassword());

        String userId = user.get("id");
        String token = user.get("token");
        String systemToken = getSystemToken(userId, loginRequestDTO.getPassword());

        if (userId == null || token == null) {
            throw new CustomException(ErrorCode.INVALID_USER_INFO);
        }


        User entity = userRepository.findByKeystoneId(userId).orElse(null);
        if (entity == null) {
            userRepository.save(User.builder().keystoneId(userId).
                    keystoneUsername(loginRequestDTO.getId()).build());
        }
        else if (!entity.getKeystoneUsername().equals(loginRequestDTO.getId())) {
            entity.changeUsername(loginRequestDTO.getId());
            userRepository.save(entity);
        }

        response.addHeader("X-Subject-Token", systemToken != null ? systemToken : token);
        return LoginResponseDTO.builder()
                .isAdmin(systemToken != null)
                .projects(getProjectsWithUser(user))
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
        ResponseEntity<Map> res;
        try {
            res = restTemplate.postForEntity(url, requestEntity, Map.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_USER_INFO);
        }
        Map<String, Object> resToken = (Map<String, Object>) res.getBody().get("token");
        Map<String, Object> resUser = (Map<String, Object>) resToken.get("user");
        String userId = (String) resUser.get("id");
        String token = res.getHeaders().getFirst("X-Subject-Token");

        return Map.of("id", userId,
                    "token", token);
    }

    private String getSystemToken(String id, String password) {

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
                "                    \"id\": \"" + id + "\",\n" +
                "                    \"password\": \"" + password + "\"\n" +
                "                }\n" +
                "            }\n" +
                "        },\n" +
                "        \"scope\": {\n" +
                "            \"system\": {\n" +
                "                \"all\": true\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        HttpEntity<String> requestEntity;
        ResponseEntity<Map> res;
        try {
            requestEntity = new HttpEntity<>(requestBody, headers);
            res = restTemplate.postForEntity(url, requestEntity, Map.class);
        } catch (Exception e) {
            return null;
        }

        Map<String, Object> resToken = (Map<String, Object>) res.getBody().get("token");
        Map<String, Object> resUser = (Map<String, Object>) resToken.get("user");
        String userId = (String) resUser.get("id");
        String token = res.getHeaders().getFirst("X-Subject-Token");

        return token;
    }

    private String getProjectToken(String unscopedToken, String projectId) {

        String url = keystone + "/auth/tokens";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        String requestBody = "{\n" +
                "    \"auth\": {\n" +
                "        \"identity\": {\n" +
                "            \"methods\": [\n" +
                "                \"token\"\n" +
                "            ],\n" +
                "            \"token\": {\n" +
                "                \"id\": \"" + unscopedToken +"\"\n" +
                "            }\n" +
                "        },\n" +
                "        \"scope\": {\n" +
                "            \"project\": {\n" +
                "                \"id\": \""+ projectId +"\"\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}";

        HttpEntity<String> requestEntity;
        ResponseEntity<Map> res;
        try {
            requestEntity = new HttpEntity<>(requestBody, headers);
            res = restTemplate.postForEntity(url, requestEntity, Map.class);
        } catch (HttpClientErrorException.Forbidden e) {
            return unscopedToken;
        }
        catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Map<String, Object> resToken = (Map<String, Object>) res.getBody().get("token");
        Map<String, Object> resUser = (Map<String, Object>) resToken.get("user");
        String userId = (String) resUser.get("id");
        String token = res.getHeaders().getFirst("X-Subject-Token");

        return token;
    }


    // 특정 사용자의 특정 프로젝트 내 최고 권한 반환
    public Map<String, String> getBestRoleWithinProject(String token, String projectId) throws JsonProcessingException {

        return getBestRoleWithinProject(Map.of(
                "id", validateTokenAndGetUserId(token),
                "token", getProjectToken(token, projectId)),
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
        headers.set("X-Auth-Token", token);

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
    public List<IdAndNameDTO> getProjectsWithUser(Map<String, String> user) throws JsonProcessingException {
        String userId = user.get("id");
        String token = user.get("token");
        if (userId == null || token == null) {
            throw new CustomException(ErrorCode.INVALID_USER_INFO);
        }

        String url = keystone + "/users/" + userId + "/projects";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", token);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

        JsonNode node = objectMapper.readTree(res.getBody());
        ArrayNode arrayNode = (ArrayNode) node.get("projects");

        List<IdAndNameDTO> lists = new ArrayList<>();

        for (JsonNode assignment : arrayNode) {
            String projectId = assignment.path("id").asText();
            String projectName = assignment.path("name").asText();
            lists.add(new IdAndNameDTO(projectId, projectName));
        }
        return lists;
    }

    public String validateTokenAndGetUserId(String token) throws JsonProcessingException {
        String url = keystone + "/auth/tokens";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", token);
        headers.set("X-Subject-Token", token);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> res;
        try {
            res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return objectMapper.readTree(res.getBody()).path("token").path("user").path("id").asText();

    }

    public List<IdAndNameDTO> getAllProjects(String token) throws JsonProcessingException {
        String url = keystone + "/projects";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", token);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> res;
        try {
            res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        JsonNode node = objectMapper.readTree(res.getBody());
        ArrayNode arrayNode = (ArrayNode) node.get("projects");

        List<IdAndNameDTO> lists = new ArrayList<>();

        for (JsonNode assignment : arrayNode) {
            String projectId = assignment.path("id").asText();
            String projectName = assignment.path("name").asText();
            lists.add(new IdAndNameDTO(projectId, projectName));
        }

        return lists;

    }

    public void validateProjectAuth(List<String> projects, String projectId) {
        if (projects != null && !projects.contains(projectId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
        }
    }

    public Boolean isAdmin(Map<String, String> user) throws JsonProcessingException {
        String url = keystone + "/role_assignments?user.id=" + user.get("id") + "&scope.system&include_names";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", user.get("token"));
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> res;
        try {
            res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        } catch (Exception e) {
            return false;
        }
        JsonNode node = objectMapper.readTree(res.getBody()).path("role_assignments");
        String system_all = node.path("scope").path("system").path("all").asText();
        String role = node.path("role").path("name").asText();
        if (system_all.equals("true") && role.equals("admin")) {
            return true;
        }
        return false;

    }
}
