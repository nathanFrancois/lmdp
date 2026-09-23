package fr.lmdp.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Corps de requête utilisé pour créer ou mettre à jour un produit via l'API d'administration
 * (et réutilisé par les formulaires du back-office /admin).
 */
public record ProductRequest(
        String id,

        @NotBlank(message = "Le nom est obligatoire")
        String name,

        @NotBlank(message = "La description est obligatoire")
        String description,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.0", inclusive = true, message = "Le prix doit être positif")
        BigDecimal price,

        @NotNull(message = "Le stock est obligatoire")
        @Min(value = 0, message = "Le stock ne peut pas être négatif")
        Integer stock,

        boolean available
) {
}

