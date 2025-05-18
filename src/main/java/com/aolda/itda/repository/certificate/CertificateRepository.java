package com.aolda.itda.repository.certificate;

import com.aolda.itda.entity.certificate.Certificate;
import com.aolda.itda.entity.routing.Routing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    // 단건 조회 (Soft Delete 고려)
    Optional<Certificate> findByCertificateIdAndIsDeleted(Long certificateId, Boolean isDeleted);

    // 프로젝트별 목록 조회 (Soft Delete 고려)
    List<Certificate> findByProjectIdAndIsDeleted(String projectId, Boolean isDeleted);

    // 만료일이 주어진 날짜 이전인(=만료 30일 이내) 인증서 조회
    //List<Certificate> findByExpiresAtBeforeAndIsDeleted(LocalDateTime date, Boolean isDeleted);

    // 1) domain 필터링용 메서드
    List<Certificate> findByProjectIdAndDomainContainingAndIsDeleted(
            String projectId, String domain, Boolean isDeleted);

    // 3) 만료 30일 이내 대상 조회
    List<Certificate> findByExpiresAtBeforeAndIsDeleted(
            LocalDateTime date, Boolean isDeleted);

    @Query("SELECT r FROM Routing r WHERE r.projectId = ?1 AND r.isDeleted = ?3 AND (r.domain LIKE %?2% OR r.instanceIp LIKE %?2% OR r.name LIKE %?2%)")
    List<Certificate> findWithSearch(String projectId, String query, Boolean isDeleted);
}
