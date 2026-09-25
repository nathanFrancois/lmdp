package fr.lmdp.payment;

import com.onlinepayments.communication.RequestHeader;
import com.onlinepayments.domain.WebhooksEvent;
import com.onlinepayments.webhooks.SignatureValidationException;
import com.onlinepayments.webhooks.WebhooksHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Point d'entrée des notifications CAWL. La signature est vérifiée sur les octets
 * exacts reçus, avant toute désérialisation ou mise à jour de commande.
 */
@RestController
public class PaymentWebhookController {

    public static final String WEBHOOK_PATH = "/api/payments/cawl/webhook";

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentService paymentService;
    private final WebhooksHelper webhooksHelper;

    public PaymentWebhookController(PaymentService paymentService, WebhooksHelper webhooksHelper) {
        this.paymentService = paymentService;
        this.webhooksHelper = webhooksHelper;
    }

    @PostMapping(WEBHOOK_PATH)
    public ResponseEntity<Void> receive(@RequestBody byte[] rawBody,
                                        @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers) {
        WebhooksEvent event = verify(rawBody, headers);
        try {
            paymentService.handleWebhook(event);
        } catch (PaymentWebhookException e) {
            log.warn("Notification CAWL rejetée : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.noContent().build();
    }

    private WebhooksEvent verify(byte[] rawBody, HttpHeaders headers) {
        try {
            return webhooksHelper.unmarshal(rawBody, toSdkHeaders(headers));
        } catch (SignatureValidationException e) {
            log.warn("Notification CAWL avec signature invalide rejetée");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private static List<RequestHeader> toSdkHeaders(HttpHeaders headers) {
        return headers.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(value -> new RequestHeader(entry.getKey(), value)))
                .toList();
    }
}

