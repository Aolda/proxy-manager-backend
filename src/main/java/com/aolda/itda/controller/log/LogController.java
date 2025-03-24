package com.aolda.itda.controller.log;

import com.aolda.itda.service.log.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
public class LogController {

    private final LogService logService;

    @GetMapping("/log")
    public ResponseEntity<Object> view(@RequestParam Long logId, HttpServletRequest request) {
        return ResponseEntity.ok(logService.getLog(logId, (List<String>) request.getAttribute("projects")));
    }

    @GetMapping("/logs")
    public ResponseEntity<Object> lists(@RequestParam(required = false) String projectId
                                        ,@RequestParam(required = false) String type
                                        ,@RequestParam(required = false) String username
                                        ,@RequestParam(required = false) String action
                                        ,@RequestParam(defaultValue = "false") boolean isASC
                                        ,@PageableDefault(size = 10) Pageable pageable
                                        ,HttpServletRequest request) {
        return ResponseEntity.ok(logService.getLogs(projectId, type, username, action, isASC, pageable,
                (Map<String, String>) request.getAttribute("user")));
    }

}
