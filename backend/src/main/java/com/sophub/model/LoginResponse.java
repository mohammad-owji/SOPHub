package com.sophub.model;

public class LoginResponse {
    private String token;
    private String benutzername;
    private String vorname;
    private String name;
    private String rolle;

    public LoginResponse(String token, String benutzername, String vorname, String name, String rolle) {
        this.token = token;
        this.benutzername = benutzername;
        this.vorname = vorname;
        this.name = name;
        this.rolle = rolle;
    }

    public String getToken() { return token; }
    public String getBenutzername() { return benutzername; }
    public String getVorname() { return vorname; }
    public String getName() { return name; }
    public String getRolle() { return rolle; }
}
