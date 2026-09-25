package fr.lmdp.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Adresse de livraison, renseignée uniquement si le client choisit l'expédition. */
@Embeddable
public class ShippingAddress {

    @Column(name = "shipping_street", length = 200)
    private String street;

    @Column(name = "shipping_complement", length = 200)
    private String complement;

    @Column(name = "shipping_postal_code", length = 20)
    private String postalCode;

    @Column(name = "shipping_city", length = 100)
    private String city;

    @Column(name = "shipping_country", length = 2)
    private String countryCode;

    protected ShippingAddress() {
        // Requis par JPA/Hibernate.
    }

    public ShippingAddress(String street, String complement, String postalCode, String city, String countryCode) {
        this.street = street;
        this.complement = complement;
        this.postalCode = postalCode;
        this.city = city;
        this.countryCode = countryCode;
    }

    public String getStreet() {
        return street;
    }

    public String getComplement() {
        return complement;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isEmpty() {
        return street == null || street.isBlank();
    }
}

