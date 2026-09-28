package fr.lmdp.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envoi d'e-mails via l'API transactionnelle de Brevo.
 *
 * @see <a href="https://developers.brevo.com/reference/sendtransacemail">Documentation Brevo</a>
 */
public class BrevoEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);
    private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final BrevoProperties properties;

    public BrevoEmailSender(RestClient.Builder restClientBuilder, BrevoProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(ENDPOINT)
                .defaultHeader("api-key", properties.apiKey())
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public void send(String toEmail, String toName, String subject, String htmlContent, String textContent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", contact(properties.senderEmail(), properties.senderName()));
        body.put("to", List.of(contact(toEmail, toName)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);
        if (textContent != null && !textContent.isBlank()) {
            body.put("textContent", textContent);
        }
        try {
            restClient.post().body(body).retrieve().toBodilessEntity();
            log.info("E-mail « {} » envoyé à {} via Brevo", subject, toEmail);
        } catch (RestClientException e) {
            throw new EmailException("Envoi de l'e-mail via Brevo impossible.", e);
        }
    }

    private Map<String, Object> contact(String email, String name) {
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("email", email);
        if (name != null && !name.isBlank()) {
            contact.put("name", name);
        }
        return contact;
    }
}



