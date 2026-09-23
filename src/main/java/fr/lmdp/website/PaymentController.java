package fr.lmdp;

import java.util.List;
import com.onlinepayments.communication.RequestHeader;
import com.onlinepayments.webhooks.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


public class PaymentController {
    private final fr.lmdp.PaymentService payments;
    private final WebhooksHelper helper;
    public PaymentController(fr.lmdp.PaymentService payments, WebhooksHelper helper) {
        this.payments = payments; this.helper = helper;
    }
    @PostMapping("/api/orders/{id}/checkout")
    public fr.lmdp.PaymentService.Checkout checkout(@PathVariable String id) {
        return payments.createCheckout(id);
    }
    @GetMapping("/api/orders/{id}/payment-status")
    public fr.lmdp.PaymentService.State status(@PathVariable String id) { return payments.state(id); }

    @PostMapping("/api/payments/cawl/webhook")
    public ResponseEntity<Void> webhook(@RequestBody byte[] rawBody, @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers) {
        List<RequestHeader> sdkHeaders = headers.entrySet().stream()
            .flatMap(entry -> entry.getValue().stream()
                .map(value -> new RequestHeader(entry.getKey(), value))).toList();
        // Aucune désérialisation/résérialisation avant le contrôle de signature.
        com.onlinepayments.domain.WebhooksEvent event;
        try {
            event = helper.unmarshal(rawBody, sdkHeaders);
        } catch (SignatureValidationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        payments.accept(event);
        return ResponseEntity.noContent().build();
    }
    @GetMapping(value = "/payment-return", produces = MediaType.TEXT_PLAIN_VALUE)
    public String returned() {
        return "Retour CAWL reçu. Consulte /api/orders/demo-001/payment-status pour la confirmation serveur.";
    }
}
