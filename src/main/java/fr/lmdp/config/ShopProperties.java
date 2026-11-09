package fr.lmdp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Paramètres de la boutique.
 *
 * @param baseUrl      URL publique du site, utilisée pour construire l'URL de retour du paiement.
 * @param shippingFee  frais d'expédition appliqués si le client choisit la livraison.
 * @param contactEmail adresse recevant les demandes de devis de personnalisation ;
 *                     si absente, l'adresse d'expédition Brevo est utilisée à la place.
 */
@ConfigurationProperties(prefix = "shop")
public record ShopProperties(String baseUrl, BigDecimal shippingFee, String contactEmail) {

    public ShopProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://lmdp.net" : baseUrl.strip().replaceAll("/+$", "");
        // Ancienne origine de production : conserver les liens sur le domaine public.
        if (baseUrl.equalsIgnoreCase("https://lmdp.onrender.com")
                || baseUrl.equalsIgnoreCase("http://lmdp.onrender.com")) {
            baseUrl = "https://lmdp.net";
        }
        shippingFee = shippingFee == null ? BigDecimal.ZERO : shippingFee;
        contactEmail = contactEmail == null ? "" : contactEmail;
    }

    public String url(String path) {
        return baseUrl + path;
    }
}

