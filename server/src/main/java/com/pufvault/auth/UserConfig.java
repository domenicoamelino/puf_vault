package com.pufvault.auth;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pufvault")
public class UserConfig {

    private Serial serial = new Serial();
    private Cors cors = new Cors();
    private List<AppUser> users = new ArrayList<>();

    public Serial getSerial() {
        return serial;
    }

    public void setSerial(Serial serial) {
        this.serial = serial;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public List<AppUser> getUsers() {
        return users;
    }

    public void setUsers(List<AppUser> users) {
        this.users = users;
    }

    public static class Serial {
        private String port;
        private int baud;
        private int timeoutMs;

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public int getBaud() {
            return baud;
        }

        public void setBaud(int baud) {
            this.baud = baud;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class AppUser {
        private String username;
        private String password;
        private String userId;
        private int maxSlots;
        private boolean animationEnabled;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public int getMaxSlots() {
            return maxSlots;
        }

        public void setMaxSlots(int maxSlots) {
            this.maxSlots = maxSlots;
        }

        public boolean isAnimationEnabled() {
            return animationEnabled;
        }

        public void setAnimationEnabled(boolean animationEnabled) {
            this.animationEnabled = animationEnabled;
        }
    }
}