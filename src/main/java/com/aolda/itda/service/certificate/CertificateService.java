package com.aolda.itda.service.certificate;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.certificate.CertificateDTO;
import com.aolda.itda.entity.certificate.Certificate;
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
    public CertificateDTO createCertificate(String projectId, CertificateDTO dto, List<String> projects) {
        // 프로젝트 권한 검증
        authService.validateProjectAuth(projects, projectId);

        // DTO 유효성 검사
        validateDTO(dto);

        Certificate certificate = Certificate.builder()
                .projectId(projectId)
                .domain(dto.getDomain())
                .description(dto.getDescription())
                .isDeleted(false)
                .build();

        certificateRepository.save(certificate);

        // 생성 로직 (certbot 호출 등) 필요 시 추가

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
}
