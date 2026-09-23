package fr.lmdp.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Objet de formulaire (mutable) utilisé pour lier les formulaires HTML du back-office.
 * Contrairement à {@code ProductRequest} (record, utilisé par l'API JSON), cette classe
 * est une JavaBean classique : c'est nécessaire pour que la case à cocher "available"
 * soit correctement liée (le décochage d'une case ne pouvant être fiabilisé qu'avec le
 * mécanisme classique de data-binding de Spring MVC, pas avec le binding par constructeur
 * utilisé pour les records).
 */
public class ProductFormData {

    private String id;

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = true, message = "Le prix doit être positif")
    private BigDecimal price;

    @NotNull(message = "Le stock est obligatoire")
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    private Integer stock;

    private boolean available;

    public ProductFormData() {
    }

    public ProductFormData(String id, String name, String description, BigDecimal price, Integer stock, boolean available) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}

