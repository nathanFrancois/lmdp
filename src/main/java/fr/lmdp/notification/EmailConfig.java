package fr.lmdp.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Câblage de l'envoi d'e-mails. Si la clé API Brevo n'est pas fournie (poste de
 * développement, environnement de test), un envoi neutralisé est utilisé : le site
 * démarre et fonctionne normalement, seuls les e-mails ne partent pas (et c'est
 * journalisé). Il suffit de renseigner {@code brevo.api-key} et {@code brevo.sender-email}
 * pour activer l'envoi réel.
 */
@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    EmailSender emailSender(RestClient.Builder restClientBuilder, BrevoProperties properties) {
        if (!properties.isConfigured()) {
            log.warn("Clé API Brevo ou expéditeur absents : l'envoi d'e-mails est désactivé sur cet environnement.");
            return new NoopEmailSender();
        }
        return new BrevoEmailSender(restClientBuilder, properties);
    }

    /** Envoi neutralisé : journalise sans échouer, tant que Brevo n'est pas configuré. */
    private static final class NoopEmailSender implements EmailSender {
        @Override
        public void send(String toEmail, String toName, String subject, String htmlContent, String textContent) {
            log.warn("Brevo non configuré : e-mail « {} » à {} non envoyé.", subject, toEmail);
        }
    }
}

