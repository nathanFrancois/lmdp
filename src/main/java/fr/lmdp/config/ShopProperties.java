package fr.lmdp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Paramètres de la boutique.
 *
 * @param baseUrl     URL publique du site, utilisée pour construire l'URL de retour du paiement.
 * @param shippingFee frais d'expédition appliqués si le client choisit la livraison.
 */
@ConfigurationProperties(prefix = "shop")
public record ShopProperties(String baseUrl, BigDecimal shippingFee) {

    public ShopProperties {
        baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        shippingFee = shippingFee == null ? BigDecimal.ZERO : shippingFee;
    }

    public String url(String path) {
        return baseUrl + path;
    }
}

