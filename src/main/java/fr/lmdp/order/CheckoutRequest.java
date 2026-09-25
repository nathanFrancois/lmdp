package fr.lmdp.order;

/** Demande de passage de commande, telle que validée avant enregistrement. */
public record CheckoutRequest(Customer customer,
                              DeliveryMethod deliveryMethod,
                              ShippingAddress shippingAddress,
                              Cart cart) {
}

