package fr.lmdp.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Demande de devis de la page "Votre objet" : le client confie son propre objet
 * et décrit l'idée de gravure qu'il a en tête.
 */
public class VotreObjetForm extends ProjetDevisForm {

    @NotBlank(message = "Merci de préciser l'objet que vous souhaitez faire graver")
    @Size(max = 200, message = "200 caractères maximum")
    private String objet;

    public String getObjet() {
        return objet;
    }

    public void setObjet(String objet) {
        this.objet = objet;
    }
}

