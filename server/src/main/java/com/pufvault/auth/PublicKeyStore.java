package com.pufvault.auth;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublicKeyStore {
    private final ConcurrentHashMap<String, String> keys = new ConcurrentHashMap<>();
    public void put(String userId, String publicKeyBase64Spki) { keys.put(userId, publicKeyBase64Spki); }
    public String require(String userId) {
        String key = keys.get(userId);
        if (key == null || key.isBlank()) throw new RuntimeException("No public key registered for current session");
        return key;
    }
}
