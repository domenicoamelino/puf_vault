package com.pufvault.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class PublicKeyStore {

    private final Map<String, String> publicKeysByUserId =
            new ConcurrentHashMap<>();

    public void save(
            String userId,
            String publicKey
    ) {
        publicKeysByUserId.put(
                userId,
                publicKey
        );
    }

    public String require(String userId) {
        String key =
                publicKeysByUserId.get(userId);

        if (key == null || key.isBlank()) {
            throw new RuntimeException(
                    "Missing public key for user "
                            + userId
            );
        }

        return key;
    }

    public boolean exists(String userId) {
        return publicKeysByUserId.containsKey(userId);
    }

    public void remove(String userId) {
        publicKeysByUserId.remove(userId);
    }

    public int size() {
        return publicKeysByUserId.size();
    }
}