package fr.lmdp.order;

import java.util.List;
import java.util.Set;

/**
 * Cycle de vie d'une commande. Le passage à {@link #PAID} n'est jamais décidé par
 * le navigateur : il découle d'une confirmation vérifiée du prestataire de paiement.
 * Les transitions suivantes sont opérées manuellement depuis le back-office.
 */
public enum OrderStatus {

    AWAITING_PAYMENT("En attente de paiement"),
    PAYMENT_FAILED("Paiement échoué"),
    PAID("Payée"),
    PREPARING("En préparation"),
    READY_FOR_PICKUP("Prête à retirer"),
    SHIPPED("Expédiée"),
    COMPLETED("Terminée"),
    CANCELLED("Annulée");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Statuts que l'administratrice peut appliquer depuis le statut courant. */
    public List<OrderStatus> nextStatuses(DeliveryMethod deliveryMethod) {
        OrderStatus fulfilled = deliveryMethod == DeliveryMethod.PICKUP ? READY_FOR_PICKUP : SHIPPED;
        return switch (this) {
            case AWAITING_PAYMENT, PAYMENT_FAILED -> List.of(CANCELLED);
            case PAID -> List.of(PREPARING, CANCELLED);
            case PREPARING -> List.of(fulfilled, CANCELLED);
            case READY_FOR_PICKUP, SHIPPED -> List.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> List.of();
        };
    }

    public boolean canMoveTo(OrderStatus target, DeliveryMethod deliveryMethod) {
        return nextStatuses(deliveryMethod).contains(target);
    }

    /** Statuts pour lesquels le stock est considéré comme consommé. */
    static final Set<OrderStatus> AFTER_PAYMENT =
            Set.of(PAID, PREPARING, READY_FOR_PICKUP, SHIPPED, COMPLETED);
}

