package com.sophub.model;

public class LoginRequest {
    private String benutzername;
    private String passwort;

    // getter
    public String getBenutzername() { return benutzername; }
    public String getPasswort() { return passwort; }

    // setter
    public void setBenutzername(String benutzername) { this.benutzername = benutzername; }
    public void setPasswort(String passwort) { this.passwort = passwort; }
}