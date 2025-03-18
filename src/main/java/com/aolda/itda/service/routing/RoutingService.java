package com.aolda.itda.service.routing;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.routing.RoutingDTO;
import com.aolda.itda.entity.certificate.Certificate;
import com.aolda.itda.entity.routing.Routing;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.certificate.CertificateRepository;
import com.aolda.itda.repository.routing.RoutingRepository;
import com.aolda.itda.service.AuthService;
import com.aolda.itda.template.RoutingTemplate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private final RoutingRepository routingRepository;
    private final CertificateRepository certificateRepository;
    private final AuthService authService;
    private final RoutingTemplate routingTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    /* Routing 조회 */
    public RoutingDTO getRouting(Long routingId, List<String> projects) {
        Routing routing = routingRepository.findByRoutingIdAndIsDeleted(routingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROUTING));

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, routing.getProjectId());

        return routing.toRoutingDTO();
    }

    /* Routing 목록 조회 */
    public PageResp<RoutingDTO> getRoutings(String projectId) {
        // project id 확인 필요
        return PageResp.<RoutingDTO>builder()
                .contents(routingRepository.findByProjectIdAndIsDeleted(projectId, false)
                        .stream()
                        .map(Routing::toRoutingDTO)
                        .toList()).build();
    }

    /* Routing 생성 */
    public void createRouting(String projectId, RoutingDTO dto) {
        /* 입력 DTO 검증 */
        validateDTO(dto);

        /* 중복 검증 */
        if (routingRepository.existsByDomainAndIsDeleted(dto.getDomain(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_DOMAIN_NAME);
        }

        /* SSL 인증서 조회 */
        Certificate certificate = dto.getCertificateId() == -1 ? null :
                certificateRepository.findById(dto.getCertificateId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_CERTIFICATE)); // isDeleted 확인 필요

        /* 라우팅 엔티티 생성 */
        Routing routing = Routing.builder()
                .isDeleted(false)
                .projectId(projectId)
                .name(dto.getName())
                .instanceIp(dto.getIp())
                .instancePort(dto.getPort())
                .domain(dto.getDomain())
                .certificate(certificate)
                .caching(dto.getCaching())
                .build();

        routingRepository.save(routing);

        /* nginx conf 파일 생성 및 예외 처리 */
        String content = routingTemplate.getRouting(dto, certificate == null ? null : certificate.formatDomain());
        String confPath = "/data/nginx/proxy_host/" + routing.getRoutingId() + ".conf";

        File file = new File(confPath);
        try {
            Path path = Paths.get(confPath);
            Files.createDirectories(path.getParent());
            if (!file.createNewFile()) {
                throw new CustomException(ErrorCode.FAIL_CREATE_CONF, "중복된 라우팅 Conf 파일이 존재합니다");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException(ErrorCode.FAIL_CREATE_CONF);
        }

        /* conf 파일 작성 및 예외 처리 */
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, false)); // 예외처리 필요
            bw.write(content);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_DELETE_CONF);
            }
            throw new CustomException(ErrorCode.FAIL_CREATE_CONF, "포트포워딩 Conf 파일을 작성하지 못했습니다");
        }

        /* nginx test */
        String url = "http://nginx:8081/nginx-api/test";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        } catch (Exception e) {
            log.error("[RestClientException] {} : {}", "Nginx Conf Test (forwarding)", e.getMessage());
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        }

        /* nginx reload */
        url = "http://nginx:8081/nginx-api/reload";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        } catch (Exception e) {
            log.error("[RestClientException] {} : {}", "Nginx Conf Reload (forwarding)", e.getMessage());
            if (file.delete()) {
                throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST, "(롤백 실패)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        }

    }

    /* Routing 수정 */
    public void editRouting(Long routingId, RoutingDTO dto, List<String> projects) {
        Routing routing = routingRepository.findByRoutingIdAndIsDeleted(routingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROUTING));

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, routing.getProjectId());

        /* 입력 DTO 검증 */
        validateDTO(dto);

        /* 중복 검증 */
        if (dto.getDomain() != null && routingRepository.existsByDomainAndIsDeleted(dto.getDomain(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_DOMAIN_NAME);
        }

        /* SSL 인증서 조회 */
        Certificate certificate = (dto.getCertificateId() == null) || (dto.getCertificateId() == -1 ) ? null :
                certificateRepository.findById(dto.getCertificateId())
                        .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_CERTIFICATE)); // isDeleted 확인 필요

        /* 파일 수정 */
        routing.edit(dto, certificate);
        String content = routingTemplate.getRouting(routing.toRoutingDTO(), certificate == null ? null : certificate.formatDomain());
        String confPath = "/data/nginx/proxy_host/" + routing.getRoutingId() + ".conf";
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
            e.printStackTrace();
            throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "라우팅 Conf 파일을 수정하지 못했습니다");
        }

        /* nginx test */
        String url = "http://nginx:8081/nginx-api/test";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            try {
                Files.copy(backup, Paths.get(confPath), StandardCopyOption.REPLACE_EXISTING
                        , StandardCopyOption.COPY_ATTRIBUTES);
                Files.delete(backup);
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "(라우팅 Conf 파일 수정)");
            }

            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        } catch (RuntimeException e) {
            log.error("[RestClientException] {} : {}", "Nginx Conf Test (forwarding)", e.getMessage());
            try {
                Files.copy(backup, Paths.get(confPath), StandardCopyOption.REPLACE_EXISTING
                        , StandardCopyOption.COPY_ATTRIBUTES);
                Files.delete(backup);
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "(라우팅 Conf 파일 수정)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        }

        /* nginx reload */
        url = "http://nginx:8081/nginx-api/reload";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            try {
                Files.copy(backup, Paths.get(confPath), StandardCopyOption.REPLACE_EXISTING
                        , StandardCopyOption.COPY_ATTRIBUTES);
                Files.delete(backup);
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "(라우팅 Conf 파일 수정)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        } catch (RuntimeException e) {
            log.error("[RestClientException] {} : {}", "Nginx Conf Reload (forwarding)", e.getMessage());
            try {
                Files.copy(backup, Paths.get(confPath), StandardCopyOption.REPLACE_EXISTING
                        , StandardCopyOption.COPY_ATTRIBUTES);
                Files.delete(backup);
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_UPDATE_CONF, "(라우팅 Conf 파일 수정)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        }

        /* DB 정보 수정 */
        routingRepository.save(routing);
    }

    /* Routing 삭제 */
    public void deleteRouting(Long routingId, List<String> projects) {
        Routing routing = routingRepository.findByRoutingIdAndIsDeleted(routingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ROUTING));

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, routing.getProjectId());

        /* 파일 삭제 */
        String confPath = "/data/nginx/proxy_host/" + routing.getRoutingId() + ".conf";
        String deletePath = confPath + ".deleted";
        try {
            Files.move(Paths.get(confPath), Paths.get(deletePath));
        } catch (IOException e) {
            throw new CustomException(ErrorCode.FAIL_DELETE_CONF);
        }

        /* nginx test */
        String url = "http://nginx:8081/nginx-api/test";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            try {
                Files.move(Paths.get(deletePath), Paths.get(confPath));
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_ROLL_BACK, "(라우팅 Conf 삭제)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        } catch (Exception e) {
            log.error("[RestClientException] {} : {}", "Nginx Conf Test (forwarding)", e.getMessage());
            try {
                Files.move(Paths.get(deletePath), Paths.get(confPath));
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_ROLL_BACK, "(라우팅 Conf 삭제)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_TEST);
        }

        /* nginx reload */
        url = "http://nginx:8081/nginx-api/reload";
        try {
            restTemplate.getForEntity(url, String.class);
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("[nginxApiException] {} : {}", e.getResponseBodyAsString(), e.getMessage());
            try {
                Files.move(Paths.get(deletePath), Paths.get(confPath));
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_ROLL_BACK, "(라우팅 Conf 삭제)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        } catch (Exception e) {
            log.error("[RestClientException] {} : {}", "Nginx Conf Reload (forwarding)", e.getMessage());
            try {
                Files.move(Paths.get(deletePath), Paths.get(confPath));
            } catch (IOException e1) {
                throw new CustomException(ErrorCode.FAIL_ROLL_BACK, "(라우팅 Conf 삭제)");
            }
            throw new CustomException(ErrorCode.FAIL_NGINX_CONF_RELOAD);
        }

        /* DB */
        routing.delete();
        routingRepository.save(routing);
    }

    private void validateDTO(RoutingDTO dto) {

        for (ConstraintViolation<RoutingDTO> violation : Validation.buildDefaultValidatorFactory().getValidator().validate(dto)) {
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT, violation.getMessage());
        }

    }
}
