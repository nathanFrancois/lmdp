package fr.lmdp.payment;

import com.onlinepayments.domain.PaymentResponse;
import com.onlinepayments.domain.WebhooksEvent;
import fr.lmdp.config.ShopProperties;
import fr.lmdp.order.Order;
import fr.lmdp.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Orchestration du paiement d'une commande avec CAWL.
 *
 * <p>Deux canaux confirment un paiement, et aucun d'eux ne fait confiance au navigateur :
 * <ul>
 *     <li>le webhook signé envoyé par CAWL (source de vérité, rejouable) ;</li>
 *     <li>l'interrogation directe de l'API au retour du client, pour un affichage immédiat.</li>
 * </ul>
 * Dans les deux cas, le montant et la devise annoncés sont comparés à ceux de la commande.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String PAYMENT_EVENT_PREFIX = "payment.";

    private final PaymentGateway gateway;
    private final OrderService orderService;
    private final PaymentEventRepository paymentEventRepository;
    private final CawlProperties cawlProperties;
    private final ShopProperties shopProperties;

    public PaymentService(PaymentGateway gateway, OrderService orderService,
                          PaymentEventRepository paymentEventRepository,
                          CawlProperties cawlProperties, ShopProperties shopProperties) {
        this.gateway = gateway;
        this.orderService = orderService;
        this.paymentEventRepository = paymentEventRepository;
        this.cawlProperties = cawlProperties;
        this.shopProperties = shopProperties;
    }

    /** Ouvre le paiement et renvoie l'URL de la page de paiement hébergée. */
    @Transactional
    public String startPayment(Order order) {
        PaymentGateway.HostedCheckoutSession session = gateway.openCheckout(order, returnUrl(order));
        orderService.attachCheckout(order, session.hostedCheckoutId());
        log.info("Commande {} : session de paiement {} ouverte", order.getReference(), session.hostedCheckoutId());
        return session.redirectUrl();
    }

    /** Rafraîchit le statut au retour du client, sans attendre le webhook. */
    @Transactional
    public void refreshPaymentStatus(Order order) {
        if (!order.isAwaitingPayment()) {
            return;
        }
        try {
            gateway.readOutcome(order.getHostedCheckoutId()).ifPresent(outcome -> apply(order, outcome));
        } catch (RuntimeException e) {
            // Le webhook confirmera : on n'empêche pas l'affichage de la page de retour.
            log.warn("Commande {} : statut de paiement non relu ({})", order.getReference(), e.getMessage());
        }
    }

    /**
     * Traite une notification CAWL dont la signature a déjà été vérifiée.
     * L'opération est idempotente : un événement déjà traité est ignoré.
     */
    @Transactional
    public void handleWebhook(WebhooksEvent event) {
        if (!cawlProperties.pspid().equals(event.getMerchantId())) {
            throw new PaymentWebhookException("Notification reçue pour un autre compte marchand.");
        }
        String eventId = event.getId();
        if (eventId == null) {
            throw new PaymentWebhookException("Notification sans identifiant d'événement.");
        }
        if (paymentEventRepository.existsById(eventId)) {
            log.debug("Notification {} déjà traitée, ignorée", eventId);
            return;
        }
        String reference = applyIfPaymentEvent(event).orElse(null);
        paymentEventRepository.save(new PaymentEvent(eventId, event.getType(), reference));
    }

    /** @return la référence de commande concernée, si l'événement portait bien sur un paiement. */
    private Optional<String> applyIfPaymentEvent(WebhooksEvent event) {
        String type = event.getType();
        PaymentResponse payment = event.getPayment();
        if (type == null || !type.startsWith(PAYMENT_EVENT_PREFIX) || payment == null) {
            return Optional.empty();
        }
        PaymentOutcome outcome = CawlPaymentGateway.toOutcome(payment);
        Optional<Order> order = findOrder(outcome);
        if (order.isEmpty()) {
            // Un même compte marchand peut servir plusieurs applications : on ignore sans échouer.
            log.warn("Notification {} sans commande correspondante (référence {})", event.getId(),
                    outcome.merchantReference());
            return Optional.empty();
        }
        apply(order.get(), outcome);
        return Optional.ofNullable(outcome.merchantReference());
    }

    private Optional<Order> findOrder(PaymentOutcome outcome) {
        if (outcome.merchantReference() == null) {
            return Optional.empty();
        }
        return orderService.findByReference(outcome.merchantReference());
    }

    private void apply(Order order, PaymentOutcome outcome) {
        ensureAmountMatches(order, outcome);
        if (outcome.isPaid()) {
            orderService.confirmPayment(order, outcome.paymentId(), outcome.status());
        } else if (outcome.isFailed()) {
            orderService.rejectPayment(order, outcome.status());
        } else {
            orderService.recordPaymentProgress(order, outcome.status());
        }
    }

    /** Un montant ou une devise différents de la commande ne doivent jamais la valider. */
    private void ensureAmountMatches(Order order, PaymentOutcome outcome) {
        boolean sameAmount = Long.valueOf(order.getTotalInMinorUnits()).equals(outcome.amountInMinorUnits());
        boolean sameCurrency = order.getCurrency().equals(outcome.currency());
        if (!sameAmount || !sameCurrency) {
            log.error("Commande {} : montant de paiement incohérent ({} {} attendu, {} {} reçu)",
                    order.getReference(), order.getTotalInMinorUnits(), order.getCurrency(),
                    outcome.amountInMinorUnits(), outcome.currency());
            throw new PaymentWebhookException("Montant de paiement incohérent avec la commande.");
        }
    }

    private String returnUrl(Order order) {
        return shopProperties.url("/commande/" + order.getReference() + "/retour");
    }
}

