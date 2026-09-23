package fr.lmdp.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Base64;

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

    // byte[] est chargé avec le produit et correspond à BYTEA dans PostgreSQL.
    // Ne pas utiliser @Lob : PostgreSQL le mapperait sur un OID, pas sur BYTEA.
    @Column(name = "image_data")
    @JsonIgnore
    private byte[] imageData;

    @Column(name = "image_content_type", length = 100)
    @JsonIgnore
    private String imageContentType;

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

    public byte[] getImageData() {
        return imageData;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImage(byte[] imageData, String imageContentType) {
        this.imageData = imageData;
        this.imageContentType = imageContentType;
    }

    @JsonIgnore
    public boolean isHasImage() {
        return imageData != null && imageData.length > 0 && imageContentType != null;
    }

    /** Image intégrée au produit retourné, sans requête HTTP supplémentaire. */
    public String getImageUrl() {
        return isHasImage()
                ? "data:" + imageContentType + ";base64," + Base64.getEncoder().encodeToString(imageData)
                : null;
    }
}
