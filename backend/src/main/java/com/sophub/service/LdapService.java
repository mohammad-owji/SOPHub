package com.sophub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.directory.DirContext;

public class LdapService {

    private static final Logger log = LoggerFactory.getLogger(LdapService.class);

    private final LdapContextSource ldapContextSource;
    private final String userDnPattern;

    public LdapService(LdapContextSource ldapContextSource, String userDnPattern) {
        this.ldapContextSource = ldapContextSource;
        this.userDnPattern = userDnPattern;
    }

    public boolean authenticate(String benutzername, String password) {
        if (benutzername == null || password == null || password.isBlank()) {
            return false;
        }
        String userDn = userDnPattern.replace("{0}", benutzername);
        try {
            DirContext ctx = ldapContextSource.getContext(userDn, password);
            LdapUtils.closeContext(ctx);
            return true;
        } catch (Exception e) {
            log.warn("LDAP-Authentifizierung fehlgeschlagen fuer '{}': {}", benutzername, e.getMessage());
            return false;
        }
    }
}
