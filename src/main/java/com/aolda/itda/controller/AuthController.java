package com.aolda.itda.controller;

import com.aolda.itda.dto.auth.LoginRequestDTO;
import com.aolda.itda.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/role")
    public ResponseEntity<Object> roleWithinProject(@RequestHeader("X-Subject-Token") String token,
                                                    @RequestParam String projectId) throws JsonProcessingException {

        return ResponseEntity.ok(authService.getBestRoleWithinProject(token, projectId));
    }
}
