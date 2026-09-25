package fr.lmdp.order;

/** Mode de remise choisi par le client au moment de la commande. */
public enum DeliveryMethod {

    PICKUP("Remise en main propre"),
    SHIPPING("Livraison à domicile");

    private final String label;

    DeliveryMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean requiresAddress() {
        return this == SHIPPING;
    }
}

