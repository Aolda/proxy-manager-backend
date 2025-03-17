package com.aolda.itda.controller.forwarding;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.service.forwarding.ForwardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Object> view(@RequestParam Long forwardingId) {
        return ResponseEntity.ok(forwardingService.getForwarding(forwardingId));
    }

    @GetMapping("/forwardings")
    public ResponseEntity<Object> lists(@RequestParam String projectId) {
        return ResponseEntity.ok(forwardingService.getForwardings(projectId));
    }

    @PatchMapping("/forwarding")
    public ResponseEntity<Object> edit(@RequestParam Long forwardingId,
                                         @RequestBody ForwardingDTO dto) {
        forwardingService.editForwarding(forwardingId, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/forwarding")
    public ResponseEntity<Object> delete(@RequestParam Long forwardingId) {
        forwardingService.deleteForwarding(forwardingId);
        return ResponseEntity.ok().build();
    }

}
