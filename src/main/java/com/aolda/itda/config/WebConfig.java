package com.aolda.itda.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) { // 스프링단에서 cors 설정
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "FETCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Authorization", "X-Refresh-Token", "Access-Control-Allow-Origin")
        ;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] excludeAuth = {"/error", "/api/auth/*" };

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludeAuth);
    }
}
