package fr.lmdp.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Champs communs aux demandes de devis "sur projet" (pages "Votre objet" et
 * "Grands projets") : coordonnées du client et description libre du projet.
 */
public abstract class ProjetDevisForm {

    @NotBlank(message = "Votre nom est obligatoire")
    private String customerName;

    @NotBlank(message = "Votre e-mail est obligatoire")
    @Email(message = "L'adresse e-mail n'est pas valide")
    private String customerEmail;

    private String customerPhone;

    @NotBlank(message = "Merci de nous décrire votre projet")
    @Size(max = 3000, message = "Le message ne doit pas dépasser 3000 caractères")
    private String message;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

