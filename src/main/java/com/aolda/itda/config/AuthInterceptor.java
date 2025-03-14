package com.aolda.itda.config;

import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
@Component
@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Subject-Token");
        if (token == null || token.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN, request.getRequestURI());
        }
        // 어드민과 일반 유저 구분 필요
        try {
            if (authService.validateTokenAndGetUserId(token) != null) {
                return true;
            }
        } catch (Exception e) {
            log.error("Token validation failed for URI {}: {}", request.getRequestURI(), e.getMessage(), e);
            throw new CustomException(ErrorCode.INVALID_TOKEN, request.getRequestURI());
        }
        throw new CustomException(ErrorCode.INVALID_TOKEN, request.getRequestURI());
    }
}
