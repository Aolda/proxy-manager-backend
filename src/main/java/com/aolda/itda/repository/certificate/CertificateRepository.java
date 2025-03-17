package com.aolda.itda.repository.certificate;

import com.aolda.itda.entity.certificate.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
}
