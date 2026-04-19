package com.translationapp.payload.response;

import java.util.UUID;

public class JwtResponse {
    private String accessToken;
    private String type = "Bearer";
    private UUID id;
    private String username;
    private String role;

    public JwtResponse(String accessToken, UUID id, String username, String role) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
