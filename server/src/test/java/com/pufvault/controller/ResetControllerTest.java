package com.pufvault.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.pufvault.auth.AuthService;
import com.pufvault.device.DeviceResetService;

class ResetControllerTest {

    @Test
    void resetAuthenticatesAndReturnsScriptOutput() throws IOException {
        AuthService authService = mock(AuthService.class);
        DeviceResetService resetService = mock(DeviceResetService.class);
        ResetController controller = new ResetController(authService, resetService);

        when(resetService.reset()).thenReturn("RESET_START\nRESET_DONE\n");

        var response = controller.resetDevice("Bearer token");

        verify(authService).requireUser("Bearer token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK RESET_TRIGGERED", response.getBody().get("response"));
        assertEquals("RESET_START\nRESET_DONE\n", response.getBody().get("output"));
    }

    @Test
    void resetReturnsControlledFailureWhenScriptFails() throws IOException {
        AuthService authService = mock(AuthService.class);
        DeviceResetService resetService = mock(DeviceResetService.class);
        ResetController controller = new ResetController(authService, resetService);

        when(resetService.reset()).thenThrow(new IOException("Reset script timed out"));

        var response = controller.resetDevice("Bearer token");

        verify(authService).requireUser("Bearer token");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("NOK RESET_FAILED", response.getBody().get("response"));
        assertEquals("Reset script timed out", response.getBody().get("error"));
    }
}
