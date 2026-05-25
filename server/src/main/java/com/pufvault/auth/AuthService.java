package com.pufvault.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserConfig config;

    public AuthService(UserConfig config) {
        this.config = config;
    }

    public AuthenticatedUser login(String username, String password) {
        Optional<UserConfig.AppUser> found = config.getUsers()
                .stream()
                .filter(u -> u.getUsername().equals(username))
                .filter(u -> u.getPassword().equals(password))
                .findFirst();

        if (found.isEmpty()) {
            throw new RuntimeException("Invalid credentials");
        }

        UserConfig.AppUser user = found.get();

        return new AuthenticatedUser(
                user.getUserId(),
                user.getUsername(),
                user.getMaxSlots(),
                user.isAnimationEnabled()
        );
    }

    public String createToken(AuthenticatedUser user) {
        String raw =
                user.userId()
                        + ":"
                        + user.username()
                        + ":"
                        + user.maxSlots()
                        + ":"
                        + user.animationEnabled()
                        + ":"
                        + Instant.now().plusSeconds(3600).getEpochSecond();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser requireUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing Authorization header");
        }

        String token = authHeader.replace("Bearer ", "");
        String decoded = new String(
                Base64.getUrlDecoder().decode(token),
                StandardCharsets.UTF_8
        );

        String[] parts = decoded.split(":");

        if (parts.length < 5) {
            throw new RuntimeException("Invalid token");
        }

        String userId = parts[0];

        UserConfig.AppUser user = config.getUsers()
                .stream()
                .filter(u -> u.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unknown user"));

        return new AuthenticatedUser(
                user.getUserId(),
                user.getUsername(),
                user.getMaxSlots(),
                user.isAnimationEnabled()
        );
    }

    public record AuthenticatedUser(
            String userId,
            String username,
            int maxSlots,
            boolean animationEnabled
    ) {
    }
}