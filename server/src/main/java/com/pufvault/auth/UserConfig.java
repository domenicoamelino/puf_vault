package com.pufvault.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "pufvault")
public class UserConfig {
    private String jwtSecret;
    private Serial serial = new Serial();
    private Cors cors = new Cors();
    private List<AppUser> users = new ArrayList<>();

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public Serial getSerial() { return serial; }
    public void setSerial(Serial serial) { this.serial = serial; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public List<AppUser> getUsers() { return users; }
    public void setUsers(List<AppUser> users) { this.users = users; }

    public static class AppUser {
        private String username;
        private String password;
        private String userId;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
    public static class Serial {
        private String port;
        private int baud;
        private int timeoutMs;
        public String getPort() { return port; }
        public void setPort(String port) { this.port = port; }
        public int getBaud() { return baud; }
        public void setBaud(int baud) { this.baud = baud; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }
}
