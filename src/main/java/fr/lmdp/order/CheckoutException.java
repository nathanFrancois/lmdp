package fr.lmdp.order;

/**
 * Erreur fonctionnelle survenant pendant la prise de commande (panier invalide,
 * produit indisponible…). Le message est destiné à être affiché au client.
 */
public class CheckoutException extends RuntimeException {

    public CheckoutException(String message) {
        super(message);
    }

    public CheckoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

