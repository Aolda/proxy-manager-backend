package com.aolda.itda.controller.routing;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.dto.routing.RoutingDTO;
import com.aolda.itda.service.routing.RoutingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoutingController {

    private final RoutingService routingService;

    @PostMapping("/routing")
    public ResponseEntity<Object> create(@RequestParam String projectId,
                                         @RequestBody RoutingDTO dto) {
        routingService.createRouting(projectId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/routing")
    public ResponseEntity<Object> view(@RequestParam Long routingId,
                                       HttpServletRequest request) {
        return ResponseEntity.ok(routingService.getRouting(routingId, (List<String>) request.getAttribute("projects")));
    }

    @GetMapping("/routings")
    public ResponseEntity<Object> lists(@RequestParam String projectId) {
        return ResponseEntity.ok(routingService.getRoutings(projectId));
    }

    @PatchMapping("/routing")
    public ResponseEntity<Object> edit(@RequestParam Long routingId,
                                       @RequestBody RoutingDTO dto,
                                       HttpServletRequest request) {
        routingService.editRouting(routingId, dto, (List<String>) request.getAttribute("projects"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/routing")
    public ResponseEntity<Object> delete(@RequestParam Long routingId,
                                         HttpServletRequest request) {
        routingService.deleteRouting(routingId, (List<String>) request.getAttribute("projects"));
        return ResponseEntity.ok().build();
    }

}
