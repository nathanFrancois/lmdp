package fr.lmdp.payment;

import com.onlinepayments.domain.Address;
import com.onlinepayments.domain.AddressPersonal;
import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.CardPaymentMethodSpecificInputBase;
import com.onlinepayments.domain.ContactDetails;
import com.onlinepayments.domain.CreateHostedCheckoutRequest;
import com.onlinepayments.domain.CreateHostedCheckoutResponse;
import com.onlinepayments.domain.GetHostedCheckoutResponse;
import com.onlinepayments.domain.HostedCheckoutSpecificInput;
import com.onlinepayments.domain.OrderReferences;
import com.onlinepayments.domain.PaymentOutput;
import com.onlinepayments.domain.PaymentResponse;
import com.onlinepayments.domain.PersonalInformation;
import com.onlinepayments.domain.PersonalName;
import com.onlinepayments.domain.Shipping;
import com.onlinepayments.merchant.MerchantClientInterface;
import fr.lmdp.order.Order;
import fr.lmdp.order.ShippingAddress;

import java.util.Optional;

/**
 * Implémentation de la passerelle au-dessus du SDK CAWL (Hosted Checkout).
 * Le client n'est jamais en contact avec les données de carte : il est redirigé
 * vers la page de paiement hébergée par le prestataire.
 */
class CawlPaymentGateway implements PaymentGateway {

    /** Capture immédiate : la commande est encaissée dès l'acceptation du paiement. */
    private static final String AUTHORIZATION_MODE_SALE = "SALE";
    private static final String LOCALE = "fr_FR";

    private final MerchantClientInterface merchant;

    CawlPaymentGateway(MerchantClientInterface merchant) {
        this.merchant = merchant;
    }

    @Override
    public HostedCheckoutSession openCheckout(Order order, String returnUrl) {
        CreateHostedCheckoutResponse response = merchant.hostedCheckout()
                .createHostedCheckout(buildRequest(order, returnUrl));
        if (response.getRedirectUrl() == null) {
            throw new PaymentException("La plateforme de paiement n'a pas fourni d'URL de redirection.");
        }
        return new HostedCheckoutSession(response.getHostedCheckoutId(), response.getRedirectUrl());
    }

    @Override
    public Optional<PaymentOutcome> readOutcome(String hostedCheckoutId) {
        if (hostedCheckoutId == null) {
            return Optional.empty();
        }
        GetHostedCheckoutResponse response = merchant.hostedCheckout().getHostedCheckout(hostedCheckoutId);
        if (response.getCreatedPaymentOutput() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.getCreatedPaymentOutput().getPayment()).map(CawlPaymentGateway::toOutcome);
    }

    /** Convertit une réponse de paiement CAWL en résultat exploitable par le domaine. */
    static PaymentOutcome toOutcome(PaymentResponse payment) {
        PaymentOutput output = payment.getPaymentOutput();
        AmountOfMoney money = output == null ? null : output.getAmountOfMoney();
        String merchantReference = output == null || output.getReferences() == null
                ? null : output.getReferences().getMerchantReference();
        return new PaymentOutcome(payment.getId(), payment.getStatus(),
                money == null ? null : money.getAmount(),
                money == null ? null : money.getCurrencyCode(),
                merchantReference);
    }

    private CreateHostedCheckoutRequest buildRequest(Order order, String returnUrl) {
        return new CreateHostedCheckoutRequest()
                .withOrder(new com.onlinepayments.domain.Order()
                        .withAmountOfMoney(new AmountOfMoney()
                                .withAmount(order.getTotalInMinorUnits())
                                .withCurrencyCode(order.getCurrency()))
                        .withReferences(new OrderReferences().withMerchantReference(order.getReference()))
                        .withCustomer(customer(order))
                        .withShipping(shipping(order)))
                .withHostedCheckoutSpecificInput(new HostedCheckoutSpecificInput()
                        .withReturnUrl(returnUrl)
                        .withLocale(LOCALE)
                        .withShowResultPage(false))
                .withCardPaymentMethodSpecificInput(new CardPaymentMethodSpecificInputBase()
                        .withAuthorizationMode(AUTHORIZATION_MODE_SALE));
    }

    private com.onlinepayments.domain.Customer customer(Order order) {
        fr.lmdp.order.Customer client = order.getCustomer();
        com.onlinepayments.domain.Customer customer = new com.onlinepayments.domain.Customer()
                .withContactDetails(new ContactDetails()
                        .withEmailAddress(client.getEmail())
                        .withPhoneNumber(client.getPhone()))
                .withPersonalInformation(new PersonalInformation()
                        .withName(new PersonalName()
                                .withFirstName(client.getFirstName())
                                .withSurname(client.getLastName())));
        ShippingAddress address = order.getShippingAddress();
        if (address != null && !address.isEmpty()) {
            customer.setBillingAddress(new Address()
                    .withStreet(address.getStreet())
                    .withAdditionalInfo(address.getComplement())
                    .withZip(address.getPostalCode())
                    .withCity(address.getCity())
                    .withCountryCode(address.getCountryCode()));
        }
        return customer;
    }

    private Shipping shipping(Order order) {
        ShippingAddress address = order.getShippingAddress();
        if (address == null || address.isEmpty()) {
            return null;
        }
        return new Shipping().withAddress(new AddressPersonal()
                .withStreet(address.getStreet())
                .withAdditionalInfo(address.getComplement())
                .withZip(address.getPostalCode())
                .withCity(address.getCity())
                .withCountryCode(address.getCountryCode())
                .withName(new PersonalName()
                        .withFirstName(order.getCustomer().getFirstName())
                        .withSurname(order.getCustomer().getLastName())));
    }
}

