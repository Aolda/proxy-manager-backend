package com.aolda.itda.service.main;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.auth.IdAndNameDTO;
import com.aolda.itda.dto.main.MainInfoDTO;
import com.aolda.itda.repository.certificate.CertificateRepository;
import com.aolda.itda.repository.forwarding.ForwardingRepository;
import com.aolda.itda.repository.routing.RoutingRepository;
import com.aolda.itda.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class MainService {

    private final AuthService authService;
    private final RoutingRepository routingRepository;
    private final ForwardingRepository forwardingRepository;
    private final CertificateRepository certificateRepository;

    /* 메인 페이지에 필요한 정보 반환 */
    public MainInfoDTO getMainInfo(String projectId, List<String> projects) {

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, projectId);

        /* 카운팅 */
        Long routing = (long) routingRepository.findByProjectIdAndIsDeleted(projectId, false).size();
        Long forwarding = (long) forwardingRepository.findByProjectIdAndIsDeleted(projectId, false).size();
        Long certificate = 0L;

        return MainInfoDTO.builder()
                .routing(routing)
                .forwarding(forwarding)
                .certificate(certificate)
                .build();
    }

    /* 접근 가능한 프로젝트 조회 */
    public PageResp<IdAndNameDTO> getAllProjects(Map<String, String> user) throws JsonProcessingException {

        List<IdAndNameDTO> projects;
        if (authService.isAdmin(user)) {
            projects = authService.getAllProjects(user.get("token"));
        }

        else {
            projects = authService.getProjectsWithUser(user);
        }

        return PageResp.<IdAndNameDTO>builder()
                .contents(projects).build();
    }
}
