package com.pufvault.controller;

import com.pufvault.auth.AuthService;
import com.pufvault.auth.PublicKeyStore;
import com.pufvault.dto.Dto;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;
    private final PublicKeyStore publicKeyStore;
    public AuthController(AuthService authService, PublicKeyStore publicKeyStore) {
        this.authService = authService; this.publicKeyStore = publicKeyStore;
    }

    @PostMapping("/login")
    public Dto.LoginResponse login(@RequestBody Dto.LoginRequest req) {
        var user = authService.authenticate(req.username(), req.password())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (req.publicKey() != null && !req.publicKey().isBlank()) publicKeyStore.put(user.getUserId(), req.publicKey());
        return new Dto.LoginResponse(authService.issueToken(user), user.getUserId());
    }

    @GetMapping("/me")
    public Map<String,String> me(@RequestHeader("Authorization") String auth) {
        var u = authService.requireUser(auth);
        return Map.of("userId", u.userId(), "username", u.username());
    }
}
