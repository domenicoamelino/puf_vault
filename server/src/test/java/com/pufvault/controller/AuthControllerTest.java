package com.pufvault.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.pufvault.auth.AuthService;
import com.pufvault.auth.PublicKeyStore;
import com.pufvault.dto.Dto;

class AuthControllerTest {

    @Test
    void loginSavesPublicKeyAndReturnsToken() {
        AuthService authService = mock(AuthService.class);
        PublicKeyStore keyStore = mock(PublicKeyStore.class);
        AuthController controller = new AuthController(authService, keyStore);

        when(authService.login("alice", "secret"))
                .thenReturn(new AuthService.AuthenticatedUser("user001", "alice", 3, true));
        when(authService.createToken(any())).thenReturn("jwt123");

        var response = controller.login(new Dto.LoginRequest("alice", "secret", "pubKey"));

        verify(keyStore).save("user001", "pubKey");
        assertEquals("jwt123", response.token());
    }
}
