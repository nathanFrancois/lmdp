package fr.lmdp.order;

import java.math.BigDecimal;

/** Frais par nombre total d'articles, quantités comprises. */
public final class ShippingRates {
    public static final BigDecimal UP_TO_THREE = new BigDecimal("6.90");
    public static final BigDecimal UP_TO_FIVE = new BigDecimal("7.90");
    public static final BigDecimal ABOVE_FIVE = new BigDecimal("9.90");

    private ShippingRates() { }

    public static BigDecimal forQuantity(long quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Le panier doit contenir au moins un article.");
        return quantity <= 3 ? UP_TO_THREE : quantity <= 5 ? UP_TO_FIVE : ABOVE_FIVE;
    }
}
