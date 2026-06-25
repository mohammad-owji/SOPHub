package com.sophub.config;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@Configuration
@Profile("local")
public class EmbeddedLdapServerConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedLdapServerConfig.class);

    @Bean(destroyMethod = "shutDown")
    public InMemoryDirectoryServer embeddedLdapServer() throws Exception {
        InMemoryDirectoryServerConfig config =
                new InMemoryDirectoryServerConfig("dc=sophub,dc=local");
        config.setSchema(null); // kein striktes Schema-Checking
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", 8389));

        InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);

        try (InputStream is = new ClassPathResource("test-users.ldif").getInputStream()) {
            server.importFromLDIF(false, new LDIFReader(is));
        }

        server.startListening();
        log.info("Embedded LDAP-Server gestartet auf Port 8389");
        return server;
    }
}
