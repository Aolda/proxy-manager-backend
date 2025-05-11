package com.aolda.itda.service.certificate;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.certificate.CertificateDTO;
import com.aolda.itda.entity.certificate.Certificate;
import com.aolda.itda.entity.certificate.Challenge;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.certificate.CertificateRepository;
import com.aolda.itda.service.AuthService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final AuthService authService;

    /** 1) 단건 조회 **/
    public CertificateDTO getCertificate(Long certificateId, List<String> projects) {
        Certificate cert = certificateRepository
                .findByCertificateIdAndIsDeleted(certificateId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));
        authService.validateProjectAuth(projects, cert.getProjectId());
        return toDTO(cert);
    }

    /** 1) 목록 조회 (domain 필터 optional) **/
    public PageResp<CertificateDTO> getCertificates(String projectId, String domain) {
        List<Certificate> list;
        if (domain != null && !domain.isBlank()) {
            list = certificateRepository
                    .findByProjectIdAndDomainContainingIgnoreCaseAndIsDeleted(
                            projectId, domain, false);
        } else {
            list = certificateRepository
                    .findByProjectIdAndIsDeleted(projectId, false);
        }
        List<CertificateDTO> dtos = list.stream()
                .map(this::toDTO)
                .toList();
        return PageResp.<CertificateDTO>builder()
                .contents(dtos)
                .build();
    }

    /** 2) 생성: expiredAt 자동 90일 설정 + 로깅 **/
    public CertificateDTO createCertificate(String projectId,
                                            CertificateDTO dto,
                                            List<String> projects) {
        log.info("createCertificate start (project={})", projectId);
        authService.validateProjectAuth(projects, projectId);
        validateDTO(dto);

        // 발급
        executeLego(dto);
        log.info("certificate issued for domain={}", dto.getDomain());

        // 엔티티 저장 (expiredAt 기본 90일 뒤)
        Certificate cert = Certificate.builder()
                .projectId(projectId)
                .domain(dto.getDomain())
                .email(dto.getEmail())
                .challenge(dto.getChallenge())
                .expiresAt(LocalDateTime.now().plusDays(90))
                .isDeleted(false)
                .apiToken(dto.getApiToken())
                .build();
        certificateRepository.save(cert);
        log.info("certificate saved (id={}, domain={})",
                cert.getCertificateId(), cert.getDomain());

        return toDTO(cert);
    }

    /** 4) 수정 **/
    public CertificateDTO editCertificate(Long certificateId,
                                          CertificateDTO dto,
                                          List<String> projects) {
        Certificate cert = certificateRepository
                .findByCertificateIdAndIsDeleted(certificateId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));
        authService.validateProjectAuth(projects, cert.getProjectId());

        if (dto.getDomain() != null)      cert.setDomain(dto.getDomain());
        if (dto.getEmail() != null)       cert.setEmail(dto.getEmail());

        certificateRepository.save(cert);
        log.info("certificate edited (id={}, domain={})",
                cert.getCertificateId(), cert.getDomain());
        return toDTO(cert);
    }

    /** 4) 삭제 + 로깅 **/
    public void deleteCertificate(Long certificateId, List<String> projects) {
        Certificate cert = certificateRepository
                .findByCertificateIdAndIsDeleted(certificateId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));
        authService.validateProjectAuth(projects, cert.getProjectId());

        cert.setIsDeleted(true);
        certificateRepository.save(cert);
        log.info("certificate deleted (id={}, domain={})",
                cert.getCertificateId(), cert.getDomain());
    }

    /** 3) 만료 30일 전 자동 갱신 배치 **/
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void renewExpiringCertificates() {
        LocalDateTime threshold = LocalDateTime.now().plusDays(30);
        List<Certificate> expiring = certificateRepository
                .findByExpiresAtBeforeAndIsDeleted(threshold, false);

        for (Certificate cert : expiring) {
            try {
                log.info("renewing (id={}, domain={})", cert.getCertificateId(), cert.getDomain());
                CertificateDTO dto = CertificateDTO.builder()
                        .domain(cert.getDomain())
                        .email(cert.getEmail())
                        .challenge(cert.getChallenge())
                        .apiToken(cert.getApiToken())
                        .build();
                executeLego(dto);

                cert.setExpiresAt(LocalDateTime.now().plusDays(90));
                certificateRepository.save(cert);
                log.info("renewed (id={}, newExpiry={})",
                        cert.getCertificateId(), cert.getExpiresAt());
            } catch (Exception e) {
                log.error("failed to renew (id={}, domain={}): {}",
                        cert.getCertificateId(), cert.getDomain(), e.getMessage());
            }
        }
    }

    /** DTO 유효성 검사 **/
    private void validateDTO(CertificateDTO dto) {
        for (ConstraintViolation<CertificateDTO> v :
                Validation.buildDefaultValidatorFactory()
                        .getValidator().validate(dto)) {
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT, v.getMessage());
        }
    }

    /** Entity→DTO 변환 **/
    private CertificateDTO toDTO(Certificate cert) {
        return CertificateDTO.builder()
                .id(cert.getCertificateId())

                .domain(cert.getDomain())
                .email(cert.getEmail())
                .challenge(cert.getChallenge())
                .expiresAt(cert.getExpiresAt())
                .createdAt(cert.getCreatedAt())
                .updatedAt(cert.getUpdatedAt())
                .isDeleted(cert.getIsDeleted())
                .apiToken(cert.getApiToken())
                .build();
    }

    /** 인증서 발급용 lego 실행 **/
    private void executeLego(CertificateDTO dto) {
        if (dto.getChallenge() == Challenge.DNS_CLOUDFLARE && dto.getApiToken() == null) {
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT,
                    "DNS_CLOUDFLARE 챌린지는 apiToken 필요");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("/usr/local/bin/lego");
        cmd.add("--accept-tos");
        cmd.add("--email");   cmd.add(dto.getEmail());
        cmd.add("--path");    cmd.add("/data/lego");

        if (dto.getChallenge() == Challenge.HTTP) {
            cmd.add("--http");
            cmd.add("--http.webroot"); cmd.add("/data/letsencrypt-acme-challenge");
        } else {
            cmd.add("--dns");  cmd.add("cloudflare");
        }

        cmd.add("--domains"); cmd.add(dto.getDomain());
        cmd.add("run");

        log.info("executing lego: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd)
                .redirectErrorStream(true);
        pb.environment().put("CF_DNS_API_TOKEN", dto.getApiToken());

        try {
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                r.lines().forEach(line -> log.info("[lego] {}", line));
            }
            if (p.waitFor() != 0) {
                throw new CustomException(ErrorCode.FAIL_CREATE_CERT,
                        "lego exit code=" + p.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            log.error("lego error", e);
            throw new CustomException(ErrorCode.FAIL_CREATE_CERT,
                    "lego 실행 실패: " + e.getMessage());
        }
    }

}
