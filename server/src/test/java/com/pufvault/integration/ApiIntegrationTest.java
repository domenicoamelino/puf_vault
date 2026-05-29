package com.pufvault.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.pufvault.auth.AuthService;
import com.pufvault.auth.PublicKeyStore;
import com.pufvault.crypto.RsaEncryptionService;
import com.pufvault.device.SerialDeviceService;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SerialDeviceService serialDeviceService;

    @MockBean
    private AuthService authService;

    @MockBean
    private PublicKeyStore publicKeyStore;

    @MockBean
    private RsaEncryptionService rsaEncryptionService;

    @Test
    void loginEndpointFlow() throws Exception {
        var user = new AuthService.AuthenticatedUser("user001", "alice", 3, true);

        when(authService.login("alice", "secret")).thenReturn(user);
        when(authService.createToken(user)).thenReturn("token-abc");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\",\"publicKey\":\"pub\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-abc"))
                .andExpect(jsonPath("$.userId").value("user001"));
    }

    @Test
    void loginPreflightAllowsProductionClientOrigin() throws Exception {
        mockMvc.perform(options("/api/login")
                        .header(HttpHeaders.ORIGIN, "https://www.domenicoamelino.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals(
                        "https://www.domenicoamelino.com",
                        result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)));
    }

    @Test
    void servicesGenerateUsesStubbedArduino() throws Exception {
        var user = new AuthService.AuthenticatedUser("user001", "alice", 3, true);
        when(authService.requireUser(eq("Bearer token"))).thenReturn(user);
        when(serialDeviceService.command("GENERATE_PASSWORD user001 github.com")).thenReturn("OK PASSWORD plain123");
        when(publicKeyStore.require("user001")).thenReturn("pub");
        when(rsaEncryptionService.encryptPassword("pub", "plain123")).thenReturn("enc");

        mockMvc.perform(post("/api/services/github.com/generate")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encryptedPassword").value("enc"));
    }
}
