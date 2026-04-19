package com.translationapp.payload.request;

import com.translationapp.model.Role;
import jakarta.validation.constraints.NotBlank;

public class SignupRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private Role role;

    public SignupRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
