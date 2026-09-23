package fr.lmdp;

import java.util.*;
import com.onlinepayments.domain.*;
import com.onlinepayments.merchant.MerchantClientInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


public class PaymentService {
    private final MerchantClientInterface merchant;
    private final String pspid, returnUrl;
    // Stockage pédagogique, perdu au redémarrage. Une seule commande de démonstration.
    private final String reference = "demo-" + UUID.randomUUID();
    private final Set<String> processedEvents = new HashSet<>();
    private Checkout checkout;
    private boolean creationAttempted;
    private String status = "UNPAID";
    private String paymentId;
    private static final long AMOUNT = 1990L; // 19,90 EUR, calculé côté serveur

    public PaymentService(MerchantClientInterface merchant, @Value("${cawl.pspid}") String pspid,
                          @Value("${cawl.return-url}") String returnUrl) {
        this.merchant = merchant; this.pspid = pspid; this.returnUrl = returnUrl;
    }
    public record Checkout(String hostedCheckoutId, String redirectUrl) {}
    public record State(String orderId, long amount, String currency, String status, String paymentId) {}

    public synchronized Checkout createCheckout(String orderId) {
        checkOrder(orderId);
        if (checkout != null) return checkout;
        // Pas de retry aveugle après un timeout : l'appel distant peut avoir réussi.
        if (creationAttempted) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Tentative ambiguë : vérifier le portail CAWL avant de recréer un paiement.");
        creationAttempted = true;
        var request = new CreateHostedCheckoutRequest()
            .withOrder(new Order().withAmountOfMoney(new AmountOfMoney()
                .withAmount(AMOUNT).withCurrencyCode("EUR"))
                .withReferences(new OrderReferences().withMerchantReference(reference)))
            .withHostedCheckoutSpecificInput(new HostedCheckoutSpecificInput().withReturnUrl(returnUrl))
            .withCardPaymentMethodSpecificInput(new CardPaymentMethodSpecificInputBase()
                .withAuthorizationMode("SALE"));
        var response = merchant.hostedCheckout().createHostedCheckout(request);
        checkout = new Checkout(response.getHostedCheckoutId(), response.getRedirectUrl());
        return checkout;
    }

    public synchronized State state(String orderId) {
        checkOrder(orderId);
        return new State(orderId, AMOUNT, "EUR", status, paymentId);
    }

    // Appelé uniquement après vérification cryptographique du corps reçu.
    public synchronized void accept(WebhooksEvent event) {
        if (!pspid.equals(event.getMerchantId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (event.getId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if (processedEvents.contains(event.getId())) return;
        if ("payment.test".equals(event.getType())) return;
        if (event.getType() == null || !event.getType().startsWith("payment.")) return;
        var payment = event.getPayment();
        if (payment == null || payment.getPaymentOutput() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var output = payment.getPaymentOutput();
        // Un compte peut envoyer des événements pour d'autres applications/commandes.
        if (output.getReferences() == null ||
            !reference.equals(output.getReferences().getMerchantReference())) return;
        var money = output.getAmountOfMoney();
        if (money == null || !Long.valueOf(AMOUNT).equals(money.getAmount()) ||
            !"EUR".equals(money.getCurrencyCode()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        if ("CAPTURED".equals(payment.getStatus())) {
            status = "PAID";
            paymentId = payment.getId();
        } else if (!"PAID".equals(status)) {
            // Ne pas assimiler une autorisation ou une capture demandée à un paiement capturé.
            status = payment.getStatus() == null ? "PENDING" : payment.getStatus();
        }
        processedEvents.add(event.getId());
        // En production : transaction DB et outbox pour déclencher la livraison une seule fois.
    }
    private void checkOrder(String id) {
        if (!"demo-001".equals(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
