package com.sophub.model;

public class LoginRequest {
    private String benutzername;
    private String password;

    // Getter
    public String getBenutzername(){ return this.benutzername; }
    public String getPassword(){ return this.password; }

    // setter
    public void setBenutzername(String benutzername) { this.benutzername = benutzername; }
    public void setPassword(String password){ this.password = password; }
}
