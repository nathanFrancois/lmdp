package fr.lmdp.payment;

import fr.lmdp.order.Order;

import java.util.Optional;

/**
 * Passerelle de paiement vue par le domaine : ouvrir une session de paiement et
 * relire son issue. Cette abstraction isole le reste de l'application du SDK CAWL.
 */
public interface PaymentGateway {

    /**
     * Ouvre une session de paiement hébergée (Hosted Checkout) pour la commande.
     *
     * @param returnUrl page du site vers laquelle le client est renvoyé après paiement.
     */
    HostedCheckoutSession openCheckout(Order order, String returnUrl);

    /** Interroge la plateforme sur l'issue d'une session de paiement. */
    Optional<PaymentOutcome> readOutcome(String hostedCheckoutId);

    /** Session de paiement ouverte chez le prestataire. */
    record HostedCheckoutSession(String hostedCheckoutId, String redirectUrl) {
    }
}

