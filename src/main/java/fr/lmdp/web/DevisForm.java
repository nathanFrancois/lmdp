package fr.lmdp.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulaire de demande de devis rempli par le client sur une fiche de la page
 * "Personnalisation". Contrairement au catalogue "Créations", ces objets ne peuvent
 * pas être ajoutés au panier : le client explique ici le motif qu'il souhaite faire
 * graver, et un e-mail est envoyé à la boutique (voir {@code DevisMailService}).
 */
public class DevisForm {

    @NotBlank
    private String productId;

    @NotBlank(message = "Votre nom est obligatoire")
    private String customerName;

    @NotBlank(message = "Votre e-mail est obligatoire")
    @Email(message = "L'adresse e-mail n'est pas valide")
    private String customerEmail;

    private String customerPhone;

    @NotBlank(message = "Merci de décrire le motif que vous souhaitez faire graver")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères")
    private String message;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

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

