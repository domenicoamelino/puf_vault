package com.pufvault.dto;

public class Dto {
    public record LoginRequest(String username, String password, String publicKey) {}
    public record LoginResponse(String token, String userId) {}
    public record AddServiceRequest(String serviceId) {}
    public record GenerateResponse(String encryptedPassword, String algorithm, String serviceId) {}
}
