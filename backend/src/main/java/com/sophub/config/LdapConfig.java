package com.sophub.config;

import com.sophub.service.LdapService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
@ConditionalOnProperty(name = "ldap.url")
public class LdapConfig {

    @Value("${ldap.url}")
    private String ldapUrl;

    @Value("${ldap.base}")
    private String ldapBase;

    @Value("${ldap.user-dn-pattern}")
    private String userDnPattern;

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource source = new LdapContextSource();
        source.setUrl(ldapUrl);
        source.setBase(ldapBase);
        return source;
    }

    @Bean
    public LdapService ldapService(LdapContextSource ldapContextSource) {
        return new LdapService(ldapContextSource, userDnPattern);
    }
}
