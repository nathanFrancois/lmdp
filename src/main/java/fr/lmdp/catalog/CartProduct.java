package fr.lmdp.catalog;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriUtils;

/** Données du panier sans chargement des octets des images. */
public record CartProduct(String id, String name, BigDecimal price, int stock, boolean hasImage) {
    public String getImageUrl() {
        return hasImage ? "/api/produits/" + UriUtils.encodePathSegment(id, StandardCharsets.UTF_8) + "/image" : null;
    }
}
