package com.pufvault.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.pufvault.auth.AuthService;
import com.pufvault.auth.PublicKeyStore;
import com.pufvault.crypto.RsaEncryptionService;
import com.pufvault.device.SerialDeviceService;
import com.pufvault.dto.Dto;

class ServiceControllerTest {

    private AuthService authService;
    private SerialDeviceService device;
    private PublicKeyStore publicKeyStore;
    private RsaEncryptionService rsa;
    private ServiceController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        device = mock(SerialDeviceService.class);
        publicKeyStore = mock(PublicKeyStore.class);
        rsa = mock(RsaEncryptionService.class);
        controller = new ServiceController(authService, device, publicKeyStore, rsa);

        when(authService.requireUser("Bearer token"))
                .thenReturn(new AuthService.AuthenticatedUser("user001", "alice", 3, true));
    }

    @Test
    void listUsesEndMarker() {
        when(device.commandMulti("LIST_SERVICES user001", "SERVICES_END")).thenReturn(List.of("SERVICES_BEGIN", "SERVICES_END"));
        Map<String, List<String>> result = controller.list("Bearer token");
        assertEquals(2, result.get("lines").size());
    }

    @Test
    void addSendsServerGeneratedCreationNonce() {
        when(device.command(anyString())).thenReturn("OK SERVICE_ADDED SLOT=0");

        Map<String, Object> result = controller.add("Bearer token", new Dto.AddServiceRequest("github.com"));

        ArgumentCaptor<String> command = ArgumentCaptor.forClass(String.class);
        verify(device).command(command.capture());

        assertEquals("OK SERVICE_ADDED SLOT=0", result.get("response"));
        assertTrue(command.getValue().matches("ADD_SERVICE user001 github\\.com \\d{8}T\\d{6}Z_[a-f0-9]{8}"));
    }

    @Test
    void generateEncryptsPassword() {
        when(device.command("GENERATE_PASSWORD user001 github.com")).thenReturn("OK PASSWORD plain123");
        when(publicKeyStore.require("user001")).thenReturn("pub");
        when(rsa.encryptPassword("pub", "plain123")).thenReturn("enc");

        Dto.GenerateResponse resp = controller.generate("Bearer token", "github.com");

        assertEquals("enc", resp.encryptedPassword());
        assertEquals("github.com", resp.serviceId());
    }

    @Test
    void generateFailsOnDeviceError() {
        when(device.command("GENERATE_PASSWORD user001 bad")).thenReturn("NOK SERVICE_NOT_FOUND");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> controller.generate("Bearer token", "bad"));
        assertEquals("NOK SERVICE_NOT_FOUND", ex.getMessage());
    }
}
