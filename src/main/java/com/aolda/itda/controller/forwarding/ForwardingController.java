package com.aolda.itda.controller.forwarding;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.service.forwarding.ForwardingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ForwardingController {

    private final ForwardingService forwardingService;

    @PostMapping("/forwarding")
    public ResponseEntity<Object> create(@RequestParam String projectId,
                                         @RequestBody ForwardingDTO dto) {
        forwardingService.createForwarding(projectId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/forwarding")
    public ResponseEntity<Object> view(@RequestParam Long forwardingId, HttpServletRequest request) {
        return ResponseEntity.ok(forwardingService.getForwarding(forwardingId, (List<String>) request.getAttribute("projects")));
    }

    @GetMapping("/forwardings")
    public ResponseEntity<Object> lists(@RequestParam String projectId) {
        return ResponseEntity.ok(forwardingService.getForwardings(projectId));
    }

    @PatchMapping("/forwarding")
    public ResponseEntity<Object> edit(@RequestParam Long forwardingId,
                                         @RequestBody ForwardingDTO dto,
                                       HttpServletRequest request) {
        forwardingService.editForwarding(forwardingId, dto, (List<String>) request.getAttribute("projects") );
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/forwarding")
    public ResponseEntity<Object> delete(@RequestParam Long forwardingId,
                                         HttpServletRequest request) {
        forwardingService.deleteForwarding(forwardingId, (List<String>) request.getAttribute("projects"));
        return ResponseEntity.ok().build();
    }

}
