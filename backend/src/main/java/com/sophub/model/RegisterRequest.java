package com.sophub.model;

public class RegisterRequest {
    private String kennung;
    private String password;
    private String email;

    public String getKennung() { return kennung; }
    public void setKennung(String kennung) { this.kennung = kennung; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
