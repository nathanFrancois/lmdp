package fr.lmdp.devis;

import fr.lmdp.catalog.Product;
import fr.lmdp.config.ShopProperties;
import fr.lmdp.notification.BrevoProperties;
import fr.lmdp.notification.EmailSender;
import fr.lmdp.notification.EmailTemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * E-mails liés à une demande de devis de personnalisation. Contrairement à la
 * confirmation de commande, un échec d'envoi doit être signalé au client (sa demande
 * ne serait sinon jamais reçue par la boutique) : voir {@code DevisController}.
 */
@Service
public class DevisMailService {

    private static final Logger log = LoggerFactory.getLogger(DevisMailService.class);

    private final EmailSender emailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final ShopProperties shopProperties;
    private final BrevoProperties brevoProperties;

    public DevisMailService(EmailSender emailSender, EmailTemplateRenderer templateRenderer,
                            ShopProperties shopProperties, BrevoProperties brevoProperties) {
        this.emailSender = emailSender;
        this.templateRenderer = templateRenderer;
        this.shopProperties = shopProperties;
        this.brevoProperties = brevoProperties;
    }

    /**
     * Envoie la demande de devis à la boutique, puis une confirmation au client.
     * L'échec d'envoi à la boutique est propagé (le client doit être informé que sa
     * demande n'est pas partie) ; l'échec de la confirmation client est seulement journalisé.
     */
    public void sendDevisRequest(Product product, String customerName, String customerEmail,
                                 String customerPhone, String message) {
        String recipient = recipientEmail();
        String html = templateRenderer.render("devis-demande", Map.of(
                "product", product,
                "customerName", customerName,
                "customerEmail", customerEmail,
                "customerPhone", StringUtils.hasText(customerPhone) ? customerPhone : "Non renseigné",
                "message", message));
        emailSender.send(recipient, "Les Merveilles de Philomène",
                "Nouvelle demande de devis — " + product.getName(),
                html, textFallback(product, customerName, customerEmail, customerPhone, message));

        try {
            String confirmationHtml = templateRenderer.render("devis-confirmation", Map.of(
                    "product", product,
                    "customerName", customerName,
                    "shopBaseUrl", shopProperties.baseUrl()));
            emailSender.send(customerEmail, customerName,
                    "Votre demande de devis a bien été reçue",
                    confirmationHtml,
                    "Merci pour votre demande concernant « " + product.getName() + " ». Nous revenons vers vous rapidement !");
        } catch (RuntimeException e) {
            log.error("Demande de devis pour {} : envoi de la confirmation client impossible", product.getId(), e);
        }
    }

    /**
     * Demande de devis "sur projet" (pages "Votre objet" et "Grands projets"), sans
     * produit du catalogue. Mêmes règles d'erreur que {@link #sendDevisRequest}.
     *
     * @param projectLabel libellé du type de projet (ex. "Votre objet")
     * @param details      informations spécifiques au projet, dans l'ordre d'affichage
     */
    public void sendProjectRequest(String projectLabel, Map<String, String> details,
                                   String customerName, String customerEmail,
                                   String customerPhone, String message) {
        String phone = StringUtils.hasText(customerPhone) ? customerPhone : "Non renseigné";
        String html = templateRenderer.render("projet-demande", Map.of(
                "projectLabel", projectLabel,
                "details", details,
                "customerName", customerName,
                "customerEmail", customerEmail,
                "customerPhone", phone,
                "message", message));

        StringBuilder text = new StringBuilder("Nouvelle demande de devis — " + projectLabel + "\n\n")
                .append("Nom : ").append(customerName).append('\n')
                .append("E-mail : ").append(customerEmail).append('\n')
                .append("Téléphone : ").append(phone).append('\n');
        details.forEach((label, value) -> text.append(label).append(" : ").append(value).append('\n'));
        text.append("\nProjet :\n").append(message);

        emailSender.send(recipientEmail(), "Les Merveilles de Philomène",
                "Nouvelle demande de devis — " + projectLabel, html, text.toString());

        try {
            String confirmationHtml = templateRenderer.render("projet-confirmation", Map.of(
                    "projectLabel", projectLabel,
                    "customerName", customerName,
                    "shopBaseUrl", shopProperties.baseUrl()));
            emailSender.send(customerEmail, customerName,
                    "Votre demande de devis a bien été reçue",
                    confirmationHtml,
                    "Merci pour votre demande (" + projectLabel + "). Nous revenons vers vous rapidement !");
        } catch (RuntimeException e) {
            log.error("Demande de devis « {} » : envoi de la confirmation client impossible", projectLabel, e);
        }
    }

    private String recipientEmail() {
        return StringUtils.hasText(shopProperties.contactEmail())
                ? shopProperties.contactEmail()
                : brevoProperties.senderEmail();
    }

    private String textFallback(Product product, String customerName, String customerEmail,
                                String customerPhone, String message) {
        return "Nouvelle demande de devis pour « " + product.getName() + " »\n\n"
                + "Nom : " + customerName + "\n"
                + "E-mail : " + customerEmail + "\n"
                + "Téléphone : " + (StringUtils.hasText(customerPhone) ? customerPhone : "Non renseigné") + "\n\n"
                + "Motif souhaité :\n" + message;
    }
}

