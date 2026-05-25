package com.pufvault.controller;

import com.pufvault.auth.AuthService;
import com.pufvault.auth.PublicKeyStore;
import com.pufvault.dto.Dto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class AuthController {

    private final AuthService authService;
    private final PublicKeyStore publicKeyStore;

    public AuthController(
            AuthService authService,
            PublicKeyStore publicKeyStore
    ) {
        this.authService = authService;
        this.publicKeyStore = publicKeyStore;
    }

    @PostMapping("/login")
    public Dto.LoginResponse login(
            @RequestBody Dto.LoginRequest request
    ) {
        var user = authService.login(
                request.username(),
                request.password()
        );

        publicKeyStore.save(
                user.userId(),
                request.publicKey()
        );

        String token = authService.createToken(user);

        return new Dto.LoginResponse(
                token,
                user.userId(),
                user.maxSlots(),
                user.animationEnabled()
        );
    }
}