package com.sophub.model;

public class RegisterRequest {
    private String benutzername;
    private String passwort;
    private String email;
    private String name;
    private String vorname;

    // getter
    public String getBenutzername() { return benutzername; }
    public String getPasswort() { return passwort; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getVorname() { return vorname; }

    // setter
    public void setBenutzername(String benutzername) { this.benutzername = benutzername; }
    public void setPasswort(String passwort) { this.passwort = passwort; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setVorname(String vorname) { this.vorname = vorname; }
}