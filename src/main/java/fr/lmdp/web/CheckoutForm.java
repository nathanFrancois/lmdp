package fr.lmdp.web;

import fr.lmdp.order.Cart;
import fr.lmdp.order.CheckoutRequest;
import fr.lmdp.order.Customer;
import fr.lmdp.order.DeliveryMethod;
import fr.lmdp.order.ShippingAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulaire de commande. Bean mutable car lié à un formulaire Thymeleaf
 * ({@code th:field}). Il ne contient aucun prix : le montant est calculé côté serveur.
 */
public class CheckoutForm {

    @NotBlank(message = "Le prénom est obligatoire.")
    @Size(max = 100)
    private String firstName = "";

    @NotBlank(message = "Le nom est obligatoire.")
    @Size(max = 100)
    private String lastName = "";

    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "L'adresse e-mail n'est pas valide.")
    @Size(max = 200)
    private String email = "";

    @Size(max = 30)
    private String phone = "";

    @NotNull(message = "Choisissez un mode de remise.")
    private DeliveryMethod deliveryMethod = DeliveryMethod.PICKUP;

    @Size(max = 200)
    private String street = "";

    @Size(max = 200)
    private String complement = "";

    @Size(max = 20)
    private String postalCode = "";

    @Size(max = 100)
    private String city = "";

    /** Contenu du panier transmis par le navigateur : {@code identifiant:quantite,…}. */
    @NotBlank(message = "Votre panier est vide.")
    private String cart = "";

    public CheckoutRequest toCheckoutRequest() {
        return new CheckoutRequest(
                new Customer(firstName.trim(), lastName.trim(), email.trim(), blankToNull(phone)),
                deliveryMethod,
                new ShippingAddress(blankToNull(street), blankToNull(complement),
                        blankToNull(postalCode), blankToNull(city), "FR"),
                Cart.parse(cart));
    }

    public boolean needsAddress() {
        return deliveryMethod != null && deliveryMethod.requiresAddress();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCart() {
        return cart;
    }

    public void setCart(String cart) {
        this.cart = cart;
    }
}

