package com.aolda.itda.controller;

import com.aolda.itda.dto.auth.LoginRequestDTO;
import com.aolda.itda.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Object> login(HttpServletResponse response,
                                        @RequestBody LoginRequestDTO loginRequestDTO) throws JsonProcessingException {

        return ResponseEntity.ok(authService.userLogin(response, loginRequestDTO));
    }
}
