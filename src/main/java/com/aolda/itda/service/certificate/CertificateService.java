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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final AuthService authService;

    /* 인증서 하나 조회 */
    public CertificateDTO getCertificate(Long certificateId, List<String> projects) {
        Certificate certificate = certificateRepository
                .findByCertificateIdAndIsDeleted(certificateId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));
        // 여기서는 ErrorCode.NOT_FOUND_CERTIFICATE 등 새로운 코드로 교체 가능

        // 프로젝트 권한 검증
        authService.validateProjectAuth(projects, certificate.getProjectId());

        return toDTO(certificate);
    }

    /* 인증서 목록 조회 */
    public PageResp<CertificateDTO> getCertificates(String projectId) {
        List<CertificateDTO> list = certificateRepository
                .findByProjectIdAndIsDeleted(projectId, false)
                .stream()
                .map(this::toDTO)
                .toList();

        return PageResp.<CertificateDTO>builder()
                .contents(list)
                .build();
    }

    /* 인증서 생성 */
    /*public CertificateDTO createCertificate(String projectId,
                                            CertificateDTO dto,
                                            List<String> projects) {
        // 프로젝트 권한 검증
        authService.validateProjectAuth(projects, projectId);
        System.out.println("2");
        // DTO 유효성 검사
        validateDTO(dto);

        Certificate certificate = Certificate.builder()
                .projectId(projectId)
                .domain(dto.getDomain())
                .description(dto.getDescription())
                .isDeleted(false)
                .build();

        certificateRepository.save(certificate);
        System.out.println("3");
        // 생성 로직 (certbot 호출 등) 필요 시 추가

        return toDTO(certificate);
    }*/
    /* 인증서 생성 + lego 호출 */
    public CertificateDTO createCertificate(String projectId, CertificateDTO dto, List<String> projects) {

        // 1) 권한 체크
        authService.validateProjectAuth(projects, projectId);

        // 2) DTO 검증
        validateDTO(dto);

        // 3) lego 명령어 구성
        ProcessBuilder pb = buildLegoProcess(dto);

        // 4) lego 실행
        int exitCode;
        try {
            Process process = pb.start();
            exitCode = process.waitFor();
            if (exitCode != 0) {
                String err = new String(process.getErrorStream().readAllBytes());
                log.error("[lego-error] {}", err);
                throw new CustomException(ErrorCode.FAIL_CREATE_CONF,
                        "lego 오류: " + err);
            }
        } catch (Exception e) {
            log.error("[lego-exec] {}", e.getMessage());
            throw new CustomException(ErrorCode.FAIL_CREATE_CONF,
                    "lego 실행 실패");
        }

        // 5) 엔티티 저장
        Certificate certificate = Certificate.builder()
                .projectId(projectId)
                .domain(dto.getDomain())
                .email(dto.getEmail())
                .challenge(dto.getChallenge())
                .expiredAt(dto.getExpiredAt())   // 필요 시 lego 출력 파싱
                .isDeleted(false)
                .description(dto.getDescription())
                .build();

        certificateRepository.save(certificate);
        return toDTO(certificate);
    }

    /* 인증서 수정  */
    public CertificateDTO editCertificate(Long certificateId, CertificateDTO dto, List<String> projects) {
        Certificate certificate = certificateRepository
                .findByCertificateIdAndIsDeleted(certificateId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));

        // 프로젝트 권한 검증
        authService.validateProjectAuth(projects, certificate.getProjectId());

        // 필요한 필드만 수정
        if (dto.getDomain() != null) {
            certificate.setDomain(dto.getDomain());
        }
        if (dto.getDescription() != null) {
            certificate.setDescription(dto.getDescription());
        }
        // 기타 수정할 필드가 있다면 추가

        // DB 저장
        certificateRepository.save(certificate);

        // 수정 로직(certbot 재발급 등) 필요 시 추가

        return toDTO(certificate);
    }

    /* 인증서 삭제 (soft delete) */
    public void deleteCertificate(Long certificateId, List<String> projects) {
        Certificate certificate = certificateRepository
                .findByCertificateIdAndIsDeleted(certificateId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));

        // 권한 검증
        authService.validateProjectAuth(projects, certificate.getProjectId());

        // soft delete
        certificate.setIsDeleted(true);
        certificateRepository.save(certificate);

        // (추가) 파일 제거 / certbot revoke 등 로직 필요 시
    }

    /* DTO 유효성 검사 */
    private void validateDTO(CertificateDTO dto) {
        for (ConstraintViolation<CertificateDTO> violation
                : Validation.buildDefaultValidatorFactory().getValidator().validate(dto)) {
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT, violation.getMessage());
        }
    }

    /* Entity -> DTO 변환 */
    private CertificateDTO toDTO(Certificate certificate) {
        return CertificateDTO.builder()
                .certificateId(certificate.getCertificateId())
                .projectId(certificate.getProjectId())
                .domain(certificate.getDomain())
                .description(certificate.getDescription())
                .isDeleted(certificate.getIsDeleted())
                .expiredAt(certificate.getExpiredAt())
                .build();
    }



    /* lego ProcessBuilder 생성 */
    private ProcessBuilder buildLegoProcess(CertificateDTO dto) {

        String basePath = "/data/lego"; // 인증서 저장 루트(볼륨)
        List<String> cmd = new ArrayList<>();
        cmd.add("/usr/local/bin/lego");
        cmd.add("--accept-tos");
        cmd.add("--email");        cmd.add(dto.getEmail());
        cmd.add("--path");         cmd.add(basePath);

        if (dto.getChallenge() == Challenge.HTTP) {
            cmd.add("--http");
            cmd.add("--http.webroot");
            cmd.add("/data/letsencrypt-acme-challenge");
        } else if (dto.getChallenge() == Challenge.DNS_CLOUDFLARE) {
            cmd.add("--dns");
            cmd.add("cloudflare");
            // CLOUDFLARE_API_TOKEN 환경변수를 컨테이너에 세팅했다고 가정
        }

        cmd.add("--domains");      cmd.add(dto.getDomain());
        cmd.add("run");            // 최초 발급(run) / renew(갱신)

        return new ProcessBuilder(cmd)
                .redirectErrorStream(true);
    }
}



// 여기서 매소드를 create로 해서 lego --accept-tos --email "email@example.com" --http --http.webroot data/letsencrypt-acme-challenge --path /data/lego --domains www.example.com run
// 이거 이메일 . 도메인 으로 넣어서 실제 인증서 연동하기!!!
// 그리고 Dto에 관리자 이메일, 인증서 완료일, 챌린지 방식 추가하기