package fr.lmdp.order;

import fr.lmdp.config.ShopProperties;
import fr.lmdp.notification.EmailTemplateRenderer;
import fr.lmdp.notification.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * E-mails liés au cycle de vie d'une commande. Un incident d'envoi (fournisseur
 * indisponible, gabarit invalide…) ne doit jamais faire échouer la confirmation
 * de paiement : il est seulement journalisé.
 */
@Service
public class OrderMailService {

    private static final Logger log = LoggerFactory.getLogger(OrderMailService.class);

    private final EmailSender emailSender;
    private final EmailTemplateRenderer templateRenderer;
    private final ShopProperties shopProperties;

    public OrderMailService(EmailSender emailSender, EmailTemplateRenderer templateRenderer,
                            ShopProperties shopProperties) {
        this.emailSender = emailSender;
        this.templateRenderer = templateRenderer;
        this.shopProperties = shopProperties;
    }

    /**
     * Confirmation envoyée dès que le paiement est validé. Elle contient un lien de
     * suivi construit à partir de la référence de commande : celle-ci est un jeton
     * aléatoire non séquentiel (voir {@link OrderReferenceGenerator}), donc impossible
     * à deviner par quelqu'un qui ne l'a pas reçue par e-mail.
     */
    public boolean sendOrderConfirmation(Order order) {
        if (!emailSender.isEnabled()) return false;
        try {
            String trackingUrl = trackingUrl(order);
            String html = templateRenderer.render("commande-confirmation", Map.of(
                    "order", order,
                    "trackingUrl", trackingUrl,
                    "shopBaseUrl", shopProperties.baseUrl()));
            emailSender.send(order.getCustomer().getEmail(), order.getCustomer().getFullName(),
                    "Votre commande " + order.getReference() + " est confirmée",
                    html, textFallback(order, trackingUrl));
            return true;
        } catch (RuntimeException e) {
            log.error("Commande {} : envoi de l'e-mail de confirmation impossible", order.getReference(), e);
            return false;
        }
    }

    private String trackingUrl(Order order) {
        return shopProperties.url("/commande/" + order.getReference());
    }

    private String textFallback(Order order, String trackingUrl) {
        return "Merci pour votre commande " + order.getReference() + " !\n\n"
                + "Suivez son avancement à tout moment grâce à ce lien personnel : " + trackingUrl;
    }
}

