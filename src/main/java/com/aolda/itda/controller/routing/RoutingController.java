package com.aolda.itda.controller.routing;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.dto.routing.RoutingDTO;
import com.aolda.itda.service.AuthService;
import com.aolda.itda.service.routing.RoutingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoutingController {

    private final RoutingService routingService;
    private final AuthService authService;

    @PostMapping("/routing")
    public ResponseEntity<Object> create(@RequestParam String projectId,
                                         @RequestBody RoutingDTO dto,
                                         HttpServletRequest request) throws IOException {

        return ResponseEntity.ok(routingService.createRouting(projectId, dto, (String) ((Map) request.getAttribute("user")).get("id")));
    }

    @GetMapping("/routing")
    public ResponseEntity<Object> view(@RequestParam Long routingId,
                                       HttpServletRequest request) {
        return ResponseEntity.ok(routingService.getRouting(routingId, (List<String>) request.getAttribute("projects")));
    }

    @GetMapping("/routings")
    public ResponseEntity<Object> lists(@RequestParam String projectId,
                                        @RequestParam(required = false) String query) {
        return ResponseEntity.ok(routingService.getRoutingsWithSearch(projectId, query));
    }

    @PatchMapping("/routing")
    public ResponseEntity<Object> edit(@RequestParam Long routingId,
                                       @RequestBody RoutingDTO dto,
                                       HttpServletRequest request) throws IOException {
        routingService.editRouting(routingId, dto, (List<String>) request.getAttribute("projects"),
                (String) ((Map) request.getAttribute("user")).get("id"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/routing")
    public ResponseEntity<Object> delete(@RequestParam Long routingId,
                                         HttpServletRequest request) {
        routingService.deleteRouting(routingId, (List<String>) request.getAttribute("projects"));
        return ResponseEntity.ok().build();
    }

}
