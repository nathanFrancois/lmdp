package fr.lmdp.web;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Demande de devis de la page "Grands projets" : une série d'objets identiques
 * gravés pour un événement (mariage, entreprise, restaurant…).
 */
public class GrandProjetForm extends ProjetDevisForm {

    public static final int QUANTITE_MIN = 10;

    public static final List<String> OCCASIONS =
            List.of("Mariage", "Anniversaire", "Restaurant / Hôtel", "Entreprise", "Autre");

    @NotBlank(message = "Merci de choisir l'occasion")
    private String occasion;

    @NotNull(message = "Merci d'indiquer la quantité souhaitée")
    @Min(value = QUANTITE_MIN, message = "Les grands projets démarrent à partir de 10 pièces")
    @Max(value = 10000, message = "Pour une telle quantité, contactez-nous directement")
    private Integer quantite;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @FutureOrPresent(message = "La date doit être à venir")
    private LocalDate dateEvenement;

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public LocalDate getDateEvenement() {
        return dateEvenement;
    }

    public void setDateEvenement(LocalDate dateEvenement) {
        this.dateEvenement = dateEvenement;
    }
}

