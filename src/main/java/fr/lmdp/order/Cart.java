package fr.lmdp.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenu du panier transmis par le navigateur : uniquement des identifiants de
 * produit et des quantités. Les prix ne transitent jamais par le client.
 * <p>Format attendu : {@code identifiant:quantite,identifiant:quantite}.
 */
public record Cart(List<CartItem> items) {

    private static final int MAX_LINES = 30;
    private static final int MAX_QUANTITY_PER_LINE = 20;

    public record CartItem(String productId, int quantity) {
    }

    public Cart {
        items = List.copyOf(items);
    }

    public static Cart parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CheckoutException("Votre panier est vide.");
        }
        Map<String, Integer> quantitiesByProduct = new LinkedHashMap<>();
        for (String entry : raw.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                throw new CheckoutException("Le contenu du panier est invalide.");
            }
            String productId = parts[0].trim();
            int quantity = parseQuantity(parts[1]);
            if (productId.isEmpty()) {
                throw new CheckoutException("Le contenu du panier est invalide.");
            }
            quantitiesByProduct.merge(productId, quantity, Integer::sum);
        }
        if (quantitiesByProduct.isEmpty() || quantitiesByProduct.size() > MAX_LINES) {
            throw new CheckoutException("Le contenu du panier est invalide.");
        }
        List<CartItem> items = new ArrayList<>();
        quantitiesByProduct.forEach((productId, quantity) ->
                items.add(new CartItem(productId, Math.min(quantity, MAX_QUANTITY_PER_LINE))));
        return new Cart(items);
    }

    private static int parseQuantity(String value) {
        try {
            int quantity = Integer.parseInt(value.trim());
            if (quantity < 1 || quantity > MAX_QUANTITY_PER_LINE) {
                throw new CheckoutException("Les quantités commandées ne sont pas valides.");
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new CheckoutException("Les quantités commandées ne sont pas valides.");
        }
    }
}

