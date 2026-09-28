package fr.lmdp.notification;

/**
 * Envoi d'e-mails transactionnels. Abstraction indépendante du fournisseur, pour que
 * chaque fonctionnalité (confirmation de commande, et futures notifications) n'ait
 * pas à connaître le prestataire utilisé (Brevo aujourd'hui).
 */
public interface EmailSender {

    /**
     * @param toEmail     destinataire.
     * @param toName      nom du destinataire, affiché dans le client mail (peut être {@code null}).
     * @param subject     objet du message.
     * @param htmlContent contenu HTML (obligatoire).
     * @param textContent contenu texte brut de secours (peut être {@code null}).
     * @throws EmailException si l'envoi échoue.
     */
    void send(String toEmail, String toName, String subject, String htmlContent, String textContent);
}

