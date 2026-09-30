package com.sophub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Schaltet @Async im Projekt ein.
 *
 * Methoden mit @Async laufen in einem eigenen Hintergrund-Thread.
 * Wir nutzen das fuer den E-Mail-Versand, damit die Webseite nicht
 * auf den (langsamen) Mailserver warten muss.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}