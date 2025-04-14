package com.aolda.itda.controller.certificate;

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
     * Body: CertificateDTO
     */
    @PostMapping("/certificate")
    public ResponseEntity<Object> create(@RequestParam String projectId,
                                         @RequestBody CertificateDTO dto,
                                         HttpServletRequest request) {
        certificateService.createCertificate(
                projectId,
                dto,
                (List<String>) request.getAttribute("projects") // Forwarding과 동일하게
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 인증서 단건 조회
     * GET /api/certificate?certificateId=xxx
     */
    @GetMapping("/certificate")
    public ResponseEntity<Object> view(@RequestParam Long certificateId,
                                       HttpServletRequest request) {
        return ResponseEntity.ok(
                certificateService.getCertificate(
                        certificateId,
                        (List<String>) request.getAttribute("projects")
                )
        );
    }

    /**
     * 인증서 목록 조회
     * GET /api/certificates?projectId=xxx
     */
    @GetMapping("/certificates")
    public ResponseEntity<Object> lists(@RequestParam String projectId) {
        return ResponseEntity.ok(
                certificateService.getCertificates(projectId)
        );
    }

    /**
     * 인증서 수정
     * PATCH /api/certificate?certificateId=xxx
     * Body: CertificateDTO
     */
    @PatchMapping("/certificate")
    public ResponseEntity<Object> edit(@RequestParam Long certificateId,
                                       @RequestBody CertificateDTO dto,
                                       HttpServletRequest request) {
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
    public ResponseEntity<Object> delete(@RequestParam Long certificateId,
                                         HttpServletRequest request) {
        certificateService.deleteCertificate(
                certificateId,
                (List<String>) request.getAttribute("projects")
        );
        return ResponseEntity.ok().build();
    }

}
