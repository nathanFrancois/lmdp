package fr.lmdp.catalog;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriUtils;

/**
 * Représente un objet en verre proposé à la vente : soit déjà gravé et vendu
 * tel quel (page « Créations »), soit proposé à la personnalisation, auquel cas
 * le client demande un devis en décrivant le motif souhaité (page « Personnalisation »).
 * Le champ {@link #customizable} détermine sur quelle page l'objet apparaît.
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

    // Détermine si l'objet apparaît sur la page "Créations" (false) ou sur la
    // page "Personnalisation" (true), où le client demande un devis plutôt que
    // d'ajouter l'objet au panier.
    @Column(nullable = false)
    private boolean customizable;

    @Column(nullable = false)
    private boolean featured;

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

    public Product(String id, String name, String description, BigDecimal price, int stock, boolean available,
                   boolean customizable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.available = available;
        this.customizable = customizable;
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

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public boolean isCustomizable() {
        return customizable;
    }

    public void setCustomizable(boolean customizable) {
        this.customizable = customizable;
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

    /** Image servie séparément, avec revalidation HTTP, sans alourdir le HTML/JSON. */
    public String getImageUrl() {
        return isHasImage()
                ? (available ? "/api/produits/" : "/api/admin/produits/")
                    + UriUtils.encodePathSegment(id, StandardCharsets.UTF_8) + "/image"
                : null;
    }
}
