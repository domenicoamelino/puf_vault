package com.pufvault.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pufvault.auth.AuthService;
import com.pufvault.device.SerialDeviceService;

@RestController
@RequestMapping("/api/device")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class DeviceController {

    private final AuthService authService;
    private final SerialDeviceService device;

    public DeviceController(
            AuthService authService,
            SerialDeviceService device
    ) {
        this.authService = authService;
        this.device = device;
    }

    @GetMapping("/status")
    public Map<String, Object> status(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        return Map.of(
                "response",
                device.command("STATUS")
        );
    }

    @GetMapping("/capability")
    public Map<String, Object> capability(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        return Map.of(
                "response",
                device.command("CAPABILITY")
        );
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> diagnostics(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        return device.diagnostics();
    }

    @GetMapping("/uart")
    public Map<String, Object> uart(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        return Map.of(
                "entries",
                device.uartLogs()
        );
    }

    @PostMapping("/reconnect")
    public Map<String, Object> reconnect(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        device.reconnect();

        return Map.of(
                "response",
                "OK RECONNECTED"
        );
    }

    @PostMapping("/ping")
    public Map<String, Object> ping(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        return Map.of(
                "response",
                device.command("PING")
        );
    }

    @PostMapping("/wipe")
    public Map<String, Object> wipe(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        String response =
                device.command("WIPE_ALL");

        return Map.of(
                "response",
                response
        );
    }
}