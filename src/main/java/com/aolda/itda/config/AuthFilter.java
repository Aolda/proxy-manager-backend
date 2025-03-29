package com.aolda.itda.config;

import com.aolda.itda.dto.auth.IdAndNameDTO;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class AuthFilter extends OncePerRequestFilter {

    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().contains("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Subject-Token");

        // 토큰 헤더 검증
        if (token == null || token.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, request.getRequestURI());
        }

        // 유효 토큰 검증
        String userId = authService.validateTokenAndGetUserId(token);
        if (userId == null) {
            log.error("Token validation failed for URI {}: {}", request.getRequestURI(), request.getRemoteAddr());
            throw new CustomException(ErrorCode.INVALID_TOKEN, request.getRequestURI());
        }

        // 프로젝트 권한 검증
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

        // 프로젝트 리스트 조회
        List<String> projects;
        if (authService.isAdmin(Map.of("id", userId, "token", token))) {
            projects = authService.getAllProjects(token).stream().map(IdAndNameDTO::getId).toList();
        } else {
            projects = authService.getProjectsWithUser(Map.of("id", userId, "token", token)).stream().map(IdAndNameDTO::getId).toList();
        }

        request.setAttribute("projects", projects);
        request.setAttribute("user", Map.of("id", userId, "token", token));

        filterChain.doFilter(request, response);
    }
}