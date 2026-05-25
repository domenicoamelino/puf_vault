package com.pufvault.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pufvault.auth.AuthService;
import com.pufvault.auth.PublicKeyStore;
import com.pufvault.crypto.RsaEncryptionService;
import com.pufvault.device.SerialDeviceService;
import com.pufvault.dto.Dto;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = {
        "http://127.0.0.1:5500",
        "http://localhost:5500"
})
public class ServiceController {

    private final AuthService authService;
    private final SerialDeviceService device;
    private final PublicKeyStore publicKeyStore;
    private final RsaEncryptionService rsa;

    public ServiceController(
            AuthService authService,
            SerialDeviceService device,
            PublicKeyStore publicKeyStore,
            RsaEncryptionService rsa
    ) {
        this.authService = authService;
        this.device = device;
        this.publicKeyStore = publicKeyStore;
        this.rsa = rsa;
    }

    @GetMapping
    public Map<String, List<String>> list(
            @RequestHeader("Authorization") String auth
    ) {

        authService.requireUser(auth);

        return Map.of(
                "lines",
                device.commandMulti(
                        "LIST_SERVICES",
                        "SERVICES_END"
                )
        );
    }

    @PostMapping
    public Map<String, Object> add(
            @RequestHeader("Authorization") String auth,
            @RequestBody Dto.AddServiceRequest req
    ) {

        authService.requireUser(auth);

        String serviceId =
                safeService(req.serviceId());

        String response =
                device.command(
                        "ADD_SERVICE "
                                + serviceId
                );

        return Map.of(
                "response",
                response
        );
    }

    @DeleteMapping("/{serviceId}")
    public Map<String, Object> delete(
            @RequestHeader("Authorization") String auth,
            @PathVariable("serviceId") String serviceId
    ) {

        authService.requireUser(auth);

        String safeServiceId =
                safeService(serviceId);

        String response =
                device.command(
                        "DELETE_SERVICE "
                                + safeServiceId
                );

        return Map.of(
                "response",
                response
        );
    }

    @PostMapping("/{serviceId}/generate")
    public Dto.GenerateResponse generate(
            @RequestHeader("Authorization") String auth,
            @PathVariable("serviceId") String serviceId
    ) {

        var user =
                authService.requireUser(auth);

        String safeServiceId =
                safeService(serviceId);

        String response =
                device.command(
                        "GENERATE_PASSWORD "
                                + safeServiceId
                );

        if (!response.startsWith("OK PASSWORD ")) {

            throw new RuntimeException(
                    response
            );
        }

        String plaintext =
                response.substring(
                        "OK PASSWORD ".length()
                ).trim();

        String encrypted =
                rsa.encryptPassword(
                        publicKeyStore.require(
                                user.userId()
                        ),
                        plaintext
                );

        return new Dto.GenerateResponse(
                encrypted,
                "RSA-OAEP-SHA256",
                safeServiceId
        );
    }

    @PostMapping("/{serviceId}/rotate")
    public Map<String, Object> rotate(
            @RequestHeader("Authorization") String auth,
            @PathVariable("serviceId") String serviceId
    ) {

        authService.requireUser(auth);

        String safeServiceId =
                safeService(serviceId);

        String response =
                device.command(
                        "ROTATE_SERVICE "
                                + safeServiceId
                );

        return Map.of(
                "response",
                response
        );
    }

    private String safeService(String s) {

        return s.replaceAll(
                "[^A-Za-z0-9._-]",
                ""
        );
    }
}