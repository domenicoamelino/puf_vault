package com.pufvault.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        UserConfig config = new UserConfig();

        UserConfig.AppUser user = new UserConfig.AppUser();
        user.setUserId("user001");
        user.setUsername("alice");
        user.setPassword("secret");
        user.setMaxSlots(3);
        user.setAnimationEnabled(true);

        config.setUsers(List.of(user));
        authService = new AuthService(config);
    }

    @Test
    void loginReturnsAuthenticatedUser() {
        var user = authService.login("alice", "secret");
        assertEquals("user001", user.userId());
        assertEquals("alice", user.username());
        assertEquals(3, user.maxSlots());
        assertTrue(user.animationEnabled());
    }

    @Test
    void loginRejectsInvalidCredentials() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login("alice", "wrong"));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void createTokenAndRequireUserRoundTrip() {
        var authUser = new AuthService.AuthenticatedUser("user001", "alice", 3, true);
        String token = authService.createToken(authUser);

        String header = "Bearer " + token;
        var resolved = authService.requireUser(header);

        assertEquals("user001", resolved.userId());
        assertEquals("alice", resolved.username());
    }

    @Test
    void requireUserRejectsMalformedToken() {
        String malformed = Base64.getUrlEncoder().withoutPadding().encodeToString("a:b".getBytes());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.requireUser("Bearer " + malformed));
        assertEquals("Invalid token", ex.getMessage());
    }
}
