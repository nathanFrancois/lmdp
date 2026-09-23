package fr.lmdp.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Représente un objet en verre déjà gravé, proposé tel quel à la vente.
 * Aucune personnalisation n'est possible : le produit est vendu avec le motif décrit.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false)
    private int stock;

    @Column(name = "image_filename", length = 255)
    private String imageFilename;

    protected Product() {
        // Requis par JPA/Hibernate.
    }

    public Product(String id, String name, String description, BigDecimal price, int stock, boolean available) {
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }
}
