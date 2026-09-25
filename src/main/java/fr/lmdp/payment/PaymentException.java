package fr.lmdp.payment;

/** Échec technique du paiement (plateforme injoignable, réponse inattendue…). */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}

