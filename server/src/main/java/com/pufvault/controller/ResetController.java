package com.pufvault.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pufvault.auth.AuthService;
import com.pufvault.device.DeviceResetService;

@RestController
@RequestMapping("/api")
public class ResetController {

    private final AuthService authService;
    private final DeviceResetService deviceResetService;

    public ResetController(
            AuthService authService,
            DeviceResetService deviceResetService
    ) {
        this.authService = authService;
        this.deviceResetService = deviceResetService;
    }

    @PostMapping("/reset_device")
    public ResponseEntity<Map<String, String>> resetDevice(
            @RequestHeader("Authorization") String auth
    ) {
        authService.requireUser(auth);

        try {
            String output = deviceResetService.reset();

            return ResponseEntity.ok(Map.of(
                    "response", "OK RESET_TRIGGERED",
                    "output", output
            ));
        } catch (Exception e) {
            Map<String, String> response = new LinkedHashMap<>();
            response.put("response", "NOK RESET_FAILED");
            response.put("error", e.getMessage() == null ? "Unknown reset error" : e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
