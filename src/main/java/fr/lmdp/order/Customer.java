package fr.lmdp.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Coordonnées du client, saisies à la commande (pas de compte client sur le site). */
@Embeddable
public class Customer {

    @Column(name = "customer_first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "customer_last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "customer_email", nullable = false, length = 200)
    private String email;

    @Column(name = "customer_phone", length = 30)
    private String phone;

    protected Customer() {
        // Requis par JPA/Hibernate.
    }

    public Customer(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

