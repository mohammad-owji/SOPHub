package com.sophub.model;

public class LoginRequest {
    private String kennung;
    private String password;

    // Getter und Setter
    public String getKennung(){ return this.kennung; }
    public String getPassword(){ return this.password; }

    public void setKennung(String kennung) { this.kennung = kennung; }
    public void setPassword(String password){ this.password = password; }
}
