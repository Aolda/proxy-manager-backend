package com.aolda.itda.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Request Body를 읽을 수 있도록 래핑
        ContentCachingRequestWrapper cachingRequest = new ContentCachingRequestWrapper(request);
        filterChain.doFilter(cachingRequest, response);

        // 로그 기록
        logRequest(cachingRequest);
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String ip = request.getRemoteAddr();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String body = getRequestBody(request);

        logger.info("IP: {}, Method: {}, URI: {}, Query Params: {}, Request Body: {}",
                ip, method, uri, (queryString != null ? queryString : "None"),
                (!body.isEmpty() ? body : "None"));
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        return (buf.length > 0) ? new String(buf) : "";
    }
}
