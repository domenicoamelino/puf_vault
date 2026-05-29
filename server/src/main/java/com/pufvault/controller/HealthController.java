package com.pufvault.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class HealthController {

    @GetMapping
    public Map<String, Object> health() {

        return Map.of(
                "server", "OK",
                "status", "OK",
                "service", "PUF Vault Server",
                "timestamp", Instant.now().toString()
        );
    }
}