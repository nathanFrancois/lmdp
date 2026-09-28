package fr.lmdp.notification;

/** Échec d'envoi d'un e-mail (fournisseur indisponible, requête rejetée, etc.). */
public class EmailException extends RuntimeException {

    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }
}

