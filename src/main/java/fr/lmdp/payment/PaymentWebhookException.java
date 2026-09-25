package fr.lmdp.payment;

/** Notification de paiement refusée : contenu incohérent ou compte marchand inattendu. */
public class PaymentWebhookException extends RuntimeException {

    public PaymentWebhookException(String message) {
        super(message);
    }
}

