package fr.lmdp.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Identifiants d'accès à Brevo (anciennement Sendinblue), utilisés pour l'envoi des
 * e-mails transactionnels (confirmation de commande, et futures notifications).
 *
 * <p>La clé API se crée dans Brevo sous <em>Paramètres &gt; Clés API (SMTP &amp; API)</em>.
 * L'adresse d'expédition doit être un expéditeur vérifié dans Brevo
 * (<em>Paramètres &gt; Expéditeurs, domaines &amp; dédiabolisation</em>).
 *
 * @param apiKey      clé API Brevo (jamais versionnée, fournie par variable d'environnement).
 * @param senderEmail adresse e-mail d'expédition, vérifiée côté Brevo.
 * @param senderName  nom affiché comme expéditeur.
 */
@ConfigurationProperties(prefix = "brevo")
public record BrevoProperties(String apiKey, String senderEmail, String senderName) {

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(senderEmail);
    }
}

