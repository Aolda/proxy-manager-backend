package com.aolda.itda.service.forwarding;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.entity.forwarding.Forwarding;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.forwarding.ForwardingRepository;
import com.aolda.itda.service.AuthService;
import com.aolda.itda.template.ForwardingTemplate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ForwardingService {

    @Value("${spring.server.base-ip}")
    private String serverBaseIp;
    @Value("${nginx.server.address}")
    private String nginxAddress;
    private final ForwardingTemplate forwardingTemplate;
    private final ForwardingRepository forwardingRepository;
    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    /* 포트포워딩 정보 조회 */
    public ForwardingDTO getForwarding(Long forwardingId, List<String> projects) {
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(forwardingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, forwarding.getProjectId());

        return forwarding.toForwardingDTO();
    }

    /* 포트포워딩 목록 조회 + 검색 */
    public PageResp<ForwardingDTO> getForwardingsWithSearch(String projectId, String query) {

        /* 입력 검증 */
        if (query == null || query.isBlank()) {
            return PageResp.<ForwardingDTO>builder()
                    .contents(forwardingRepository.findByProjectIdAndIsDeleted(projectId, false)
                            .stream()
                            .map(Forwarding::toForwardingDTO)
                            .toList()).build();
        }

        return PageResp.<ForwardingDTO>builder()
                .contents(forwardingRepository.findWithSearch(projectId, query, false)
                        .stream()
                        .map(Forwarding::toForwardingDTO)
                        .toList()).build();
    }

    /* 포트포워딩 생성 */
    public ForwardingDTO createForwarding(String projectId, ForwardingDTO dto, String userID) {

        /* 입력 DTO 검증 */
        validateDTO(dto, userID);

        /* 중복 검증 */
        if (forwardingRepository.existsByInstanceIpAndInstancePortAndIsDeleted(dto.getInstanceIp(), dto.getInstancePort(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_INSTANCE_INFO);
        }

        if (authService.isAdmin(userID) &&
                dto.getServerPort() != null &&
                forwardingRepository.existsByServerPortAndIsDeleted(dto.getServerPort(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_SERVER_PORT);
        }

        /* 포트포워딩 엔티티 생성 */
        String serverPort = dto.getServerPort() == null ? String.valueOf(createPort()) : dto.getServerPort();
        Forwarding forwarding = Forwarding.builder()
                .isDeleted(false)
                .projectId(projectId)
                .name(dto.getName())
                .serverIp(dto.getServerIp() == null ? serverBaseIp : dto.getServerIp())
                .serverPort(serverPort)
                .instanceIp(dto.getInstanceIp())
                .instancePort(dto.getInstancePort())
                .build();

        forwardingRepository.save(forwarding);

        /* nginx conf 파일 생성 및 예외 처리 */
        String content = forwardingTemplate.getPortForwardingWithTCP(serverPort, dto.getInstanceIp(), dto.getInstancePort(), dto.getName());
        String confPath = "/data/nginx/stream/" + forwarding.getForwardingId() + ".conf";

        File file = new File(confPath);
        try {
            Path path = Paths.get(confPath);
            Files.createDirectories(path.getParent());
            if (!file.createNewFile()) {
                throw new CustomException(ErrorCode.FAIL_CREATE_CONF, "중복된 포트포워딩 Conf 파일이 존재합니다");
            }
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FAIL_CREATE_CONF);
        }

        /* conf 파일 작성 및 예외 처리 */
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, false)); // 예외처리 필요
            bw.write(content);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_DELETE_CONF);
            }
            throw new CustomException(ErrorCode.FAIL_CREATE_CONF, "포트포워딩 Conf 파일을 작성하지 못했습니다");
        }

        /* nginx test */
        String url = "http://" + nginxAddress + ":8081/nginx-api/test";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        } catch (Exception e) {
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        }

        /* nginx reload */
        url = "http://" + nginxAddress + ":8081/nginx-api/reload";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        } catch (Exception e) {
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        }
        return forwarding.toForwardingDTO();
    }

    /* 포트포워딩 정보 수정 */
    public void editForwarding(Long forwardingId, ForwardingDTO dto, List<String> projects, String userID) {
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(forwardingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, forwarding.getProjectId());

        /* 중복 검증 */
        if (dto.getServerPort() != null && forwardingRepository.existsByServerPortAndIsDeleted(dto.getServerPort(), false)
        && authService.isAdmin(userID)) {
            forwardingRepository.existsByServerPortAndIsDeleted(dto.getServerPort(), false);
            throw new CustomException(ErrorCode.DUPLICATED_SERVER_PORT);
        }

        if (!(dto.getInstanceIp() == null && dto.getInstancePort() == null) &&
                forwardingRepository.existsByInstanceIpAndInstancePortAndIsDeleted(
                        dto.getInstanceIp() == null ? forwarding.getInstanceIp() : dto.getInstanceIp()
                , dto.getInstancePort() == null ? forwarding.getInstancePort() : dto.getInstancePort()
                , false)) {
            throw new CustomException(ErrorCode.DUPLICATED_INSTANCE_INFO);
        }

        if (!(dto.getInstanceIp() == null) && !dto.getInstanceIp().startsWith("10.16.")
        && !(dto.getInstanceIp().startsWith("172.16.") && authService.isAdmin(userID)))
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT, "허용되지 않은 IP대역입니다");

        /* 파일 수정 */
        forwarding.edit(dto);
        String content = forwardingTemplate.getPortForwardingWithTCP(forwarding.getServerPort(),
                forwarding.getInstanceIp(),
                forwarding.getInstancePort(),
                forwarding.getName());
        String confPath = "/data/nginx/stream/" + forwarding.getForwardingId() + ".conf";
        File file = new File(confPath);
        if (!file.exists()) {
            throw new CustomException(ErrorCode.NOT_FOUND_FORWARDING, "Conf 파일이 존재하지 않아 수정할 수 없습니다");
        }

        Path backup;
        try {
            backup = Files.createTempFile("temp_", ".tmp");
            Files.copy(Paths.get(confPath), backup, StandardCopyOption.REPLACE_EXISTING
            , StandardCopyOption.COPY_ATTRIBUTES);

            BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));
            bw.write(content);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "포트포워딩 Conf 파일을 수정하지 못했습니다");
        }

        /* nginx test */
        String url = "http://" + nginxAddress + ":8081/nginx-api/test";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (RuntimeException e) {

            try {
                Files.copy(backup, Paths.get(confPath), StandardCopyOption.REPLACE_EXISTING
                        , StandardCopyOption.COPY_ATTRIBUTES);
                Files.delete(backup);
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "(포트포워딩 Conf 파일 수정)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        }

        /* nginx reload */
        url = "http://" + nginxAddress + ":8081/nginx-api/reload";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (RuntimeException e) {
            try {
                Files.copy(backup, Paths.get(confPath), StandardCopyOption.REPLACE_EXISTING
                , StandardCopyOption.COPY_ATTRIBUTES);
                Files.delete(backup);
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "(포트포워딩 Conf 파일 수정)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        }

        /* DB 정보 수정 */
        forwardingRepository.save(forwarding);
    }

    /* 포트포워딩 삭제 (소프트) */
    public void deleteForwarding(Long forwardingId, List<String> projects) {
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(forwardingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, forwarding.getProjectId());

        /* 파일 삭제 */
        String confPath = "/data/nginx/stream/" + forwarding.getForwardingId() + ".conf";
        String deletePath = confPath + ".deleted";
        try {
            Files.move(Paths.get(confPath), Paths.get(deletePath));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FAIL_DELETE_CONF);
        }

        /* nginx test */
        String url = "http://" + nginxAddress + ":8081/nginx-api/test";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (Exception e) {
            try {
                Files.move(Paths.get(deletePath), Paths.get(confPath));
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_ROLL_BACK, "(포트포워딩 Conf 삭제)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        }

        /* nginx reload */
        url = "http://" + nginxAddress + ":8081/nginx-api/reload";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (Exception e) {
            try {
                Files.move(Paths.get(deletePath), Paths.get(confPath));
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_ROLL_BACK, "(포트포워딩 Conf 삭제)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        }

        /* DB */
        forwarding.delete();
        forwardingRepository.save(forwarding);

    }

    /* 입력 DTO 검증 */
    private void validateDTO(ForwardingDTO dto, String userID) {

        for (ConstraintViolation<ForwardingDTO> violation : Validation.buildDefaultValidatorFactory().getValidator().validate(dto)) {
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT, violation.getMessage());
        }
        if (!dto.getInstanceIp().startsWith("10.16.") && !(dto.getInstanceIp().startsWith("172.16.") && authService.isAdmin(userID)))
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT, "허용되지 않은 IP대역입니다");

    }

    private int createPort() {
        List<Integer> usedPorts = forwardingRepository.findAllUsedServerPortsByIsDeleted(false);
        List<Integer> availablePorts = new ArrayList<>();

        for (int port = 20000; port <= 29999; port++) {
            availablePorts.add(port);
        }
        availablePorts.removeAll(usedPorts);

        if (availablePorts.isEmpty()) {
            throw new CustomException(ErrorCode.FAIL_CREATE_FORWARDING, "사용 가능한 포트가 없습니다");
        }

        int idx = (int) (Math.random() * availablePorts.size());
        return availablePorts.get(idx);
    }
}
