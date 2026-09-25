package fr.lmdp.payment;

import java.util.Set;

/**
 * Issue d'un paiement telle que rapportée par CAWL, quel que soit le canal
 * (retour navigateur ou webhook).
 *
 * @param paymentId          identifiant du paiement chez le prestataire.
 * @param status             statut brut renvoyé par la plateforme.
 * @param amountInMinorUnits montant constaté, en centimes.
 * @param currency           devise du montant constaté.
 * @param merchantReference  référence de commande transmise lors de la création du paiement.
 */
public record PaymentOutcome(String paymentId,
                             String status,
                             Long amountInMinorUnits,
                             String currency,
                             String merchantReference) {

    /**
     * Paiements encaissés. La capture est demandée immédiatement (mode SALE) :
     * {@code CAPTURE_REQUESTED} vaut donc encaissement pour la boutique.
     */
    private static final Set<String> PAID_STATUSES = Set.of("CAPTURED", "CAPTURE_REQUESTED", "PAID");

    /** Paiements définitivement non aboutis. */
    private static final Set<String> FAILED_STATUSES =
            Set.of("REJECTED", "REJECTED_CAPTURE", "CANCELLED", "REVERSED");

    public boolean isPaid() {
        return status != null && PAID_STATUSES.contains(status);
    }

    public boolean isFailed() {
        return status != null && FAILED_STATUSES.contains(status);
    }
}

