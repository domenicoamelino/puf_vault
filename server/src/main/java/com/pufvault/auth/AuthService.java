package com.pufvault.auth;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {
    private final UserConfig config;
    public AuthService(UserConfig config) { this.config = config; }

    public Optional<UserConfig.AppUser> authenticate(String username, String password) {
        return config.getUsers().stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                .findFirst();
    }

    public String issueToken(UserConfig.AppUser user) {
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();
        String payload = user.getUserId() + ":" + user.getUsername() + ":" + exp;
        String sig = hmac(payload);
        return b64(payload) + "." + sig;
    }

    public AuthUser requireUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) throw new RuntimeException("Missing bearer token");
        String token = authHeader.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length != 2) throw new RuntimeException("Invalid token");
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        if (!hmac(payload).equals(parts[1])) throw new RuntimeException("Invalid token signature");
        String[] fields = payload.split(":");
        if (fields.length != 3) throw new RuntimeException("Invalid token payload");
        if (Long.parseLong(fields[2]) < Instant.now().getEpochSecond()) throw new RuntimeException("Token expired");
        return new AuthUser(fields[0], fields[1]);
    }

    private String b64(String s) { return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    public record AuthUser(String userId, String username) {}
}
