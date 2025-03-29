package com.aolda.itda.config;

import com.aolda.itda.dto.auth.IdAndNameDTO;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Subject-Token");

        /* 토큰 헤더 검증 */
        if (token == null || token.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, request.getRequestURI());
        }

        /* 유효 토큰 검증 */
        String userId = authService.validateTokenAndGetUserId(token);
        if (userId == null) {
            log.error("Token validation failed for URI {}: {}", request.getRequestURI(), request.getRemoteAddr());
            throw new CustomException(ErrorCode.INVALID_TOKEN, request.getRequestURI());
        }

        /* 프로젝트 권한 검증 */
        String projectId = request.getParameter("projectId");
        if (projectId != null) {

            try {
                authService.getBestRoleWithinProject(token, projectId).get("role");
                if (!request.getMethod().equals("GET") && !authService.getBestRoleWithinProject(token, projectId).get("role").equals("admin")) {
                    throw new CustomException(ErrorCode.UNAUTHORIZED_USER, request.getRequestURI());

                }
            } catch (Exception e) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_USER, request.getRequestURI());
            }



        }

        /* 프로젝트 리스트 조회 */
        List<String> projects;
        if (authService.isAdmin(Map.of("id", userId, "token", token))) {
            projects = authService.getAllProjects(token).stream().map(IdAndNameDTO::getId)
                    .toList();
        }

        else {
            projects = authService.getProjectsWithUser(Map.of("id", userId, "token", token))
                    .stream().map(IdAndNameDTO::getId)
                    .toList();
        }

        request.setAttribute("projects", projects);
        request.setAttribute("user", Map.of("id", userId, "token", token));
        return true;

    }
}
