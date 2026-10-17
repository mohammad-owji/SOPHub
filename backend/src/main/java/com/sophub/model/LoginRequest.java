package com.sophub.model;

public class LoginRequest {
    private String email;
    private String passwort;

    public String getEmail() { return email; }
    public String getPasswort() { return passwort; }

    public void setEmail(String email) { this.email = email; }
    public void setPasswort(String passwort) { this.passwort = passwort; }
}