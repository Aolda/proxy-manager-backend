package com.aolda.itda.controller.main;

import com.aolda.itda.service.main.MainService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MainController {

    private final MainService mainService;

    @GetMapping("/projects")
    public ResponseEntity<Object> projects(HttpServletRequest request) throws JsonProcessingException {
        return ResponseEntity.ok(mainService.getAllProjects((Map<String, String>) request.getSession().getAttribute("user")));
    }

    @GetMapping("/main")
    public ResponseEntity<Object> mainInfo(@RequestParam String projectId, HttpServletRequest request) {
        return ResponseEntity.ok(mainService.getMainInfo(projectId, (List<String>) request.getAttribute("projects")));
    }

}
