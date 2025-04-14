package com.aolda.itda.repository.certificate;

import com.aolda.itda.entity.certificate.Certificate;
import com.aolda.itda.entity.forwarding.Forwarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    // 단건 조회 (Soft Delete 고려)
    Optional<Certificate> findByCertificateIdAndIsDeleted(Long certificateId, Boolean isDeleted);

    // 프로젝트별 목록 조회 (Soft Delete 고려)
    List<Certificate> findByProjectIdAndIsDeleted(String projectId, Boolean isDeleted);

}
