package com.aolda.itda.controller.certificate;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.certificate.CertificateDTO;
import com.aolda.itda.service.certificate.CertificateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * 인증서 생성
     * POST /api/certificate?projectId=xxx
     */
    @PostMapping("/certificate")
    public ResponseEntity<CertificateDTO> create(
            @RequestParam String projectId,
            @RequestBody CertificateDTO dto,
            HttpServletRequest request
    ) {

        return ResponseEntity.ok(certificateService.createCertificate(
                projectId,
                dto,
                (List<String>) request.getAttribute("projects")
        ));
    }

    /**
     * 인증서 단건 조회
     * GET /api/certificate?certificateId=xxx
     */
    @GetMapping("/certificate")
    public ResponseEntity<CertificateDTO> view(
            @RequestParam Long certificateId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                certificateService.getCertificate(
                        certificateId,
                        (List<String>) request.getAttribute("projects")
                )
        );
    }

    /**
     * 인증서 목록 조회 (domain 필터링 optional)
     * GET /api/certificates?projectId=xxx&domain=foo
     */
    @GetMapping("/certificates")
    public ResponseEntity<PageResp<CertificateDTO>> lists(
            @RequestParam String projectId,
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String query
    ) {

        if (query != null) {
            return ResponseEntity.ok(
                    certificateService.getCertificatesWithSearch(
                            projectId,
                            query
                    )
            );
        }
        return ResponseEntity.ok(
                certificateService.getCertificates(projectId, domain)
        );
    }

    /**
     * 인증서 수정
     * PATCH /api/certificate?certificateId=xxx
     */
    @PatchMapping("/certificate")
    public ResponseEntity<Void> edit(
            @RequestParam Long certificateId,
            @RequestBody CertificateDTO dto,
            HttpServletRequest request
    ) {
        certificateService.editCertificate(
                certificateId,
                dto,
                (List<String>) request.getAttribute("projects")
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 인증서 삭제
     * DELETE /api/certificate?certificateId=xxx
     */
    @DeleteMapping("/certificate")
    public ResponseEntity<Void> delete(
            @RequestParam Long certificateId,
            HttpServletRequest request
    ) {
        certificateService.deleteCertificate(
                certificateId,
                (List<String>) request.getAttribute("projects")
        );
        return ResponseEntity.ok().build();
    }
}
