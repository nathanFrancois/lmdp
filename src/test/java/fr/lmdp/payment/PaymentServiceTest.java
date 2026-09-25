package fr.lmdp.payment;

import com.onlinepayments.domain.AmountOfMoney;
import com.onlinepayments.domain.PaymentOutput;
import com.onlinepayments.domain.PaymentReferences;
import com.onlinepayments.domain.PaymentResponse;
import com.onlinepayments.domain.WebhooksEvent;
import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import fr.lmdp.config.ShopProperties;
import fr.lmdp.order.Cart;
import fr.lmdp.order.CheckoutRequest;
import fr.lmdp.order.Customer;
import fr.lmdp.order.DeliveryMethod;
import fr.lmdp.order.Order;
import fr.lmdp.order.OrderReferenceGenerator;
import fr.lmdp.order.OrderRepository;
import fr.lmdp.order.OrderService;
import fr.lmdp.order.OrderStatus;
import fr.lmdp.order.ShippingAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Vérifie les garde-fous du webhook : seul un paiement encaissé, du bon montant et
 * du bon compte marchand valide une commande, et un événement rejoué reste sans effet.
 */
class PaymentServiceTest {

    private static final String PSPID = "merchant-test";

    private final PaymentEventRepository eventRepository = mock(PaymentEventRepository.class);
    private final PaymentGateway gateway = mock(PaymentGateway.class);
    private PaymentService paymentService;
    private Order order;

    @BeforeEach
    void setUp() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProductService productService = mock(ProductService.class);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productService.findById("vase"))
                .thenReturn(Optional.of(new Product("vase", "Vase", "Un vase", new BigDecimal("19.90"), 5, true)));

        ShopProperties shopProperties = new ShopProperties("http://localhost:8077", BigDecimal.ZERO);
        OrderService orderService = new OrderService(orderRepository, productService,
                new OrderReferenceGenerator(), shopProperties);
        order = orderService.place(new CheckoutRequest(
                new Customer("Claire", "Martin", "claire@example.com", null),
                DeliveryMethod.PICKUP,
                new ShippingAddress(null, null, null, null, "FR"),
                Cart.parse("vase:1")));
        when(orderRepository.findByReference(order.getReference())).thenReturn(Optional.of(order));

        paymentService = new PaymentService(gateway, orderService, eventRepository,
                new CawlProperties(PSPID, "key", "secret", "wh-key", "wh-secret",
                        URI.create("https://payment.preprod.cawl-solutions.fr/"), "test"),
                shopProperties);
    }

    @Test
    void capturedPaymentPaysTheOrderOnlyOnce() {
        when(eventRepository.existsById("evt-1")).thenReturn(false, true);

        paymentService.handleWebhook(event("evt-1", "CAPTURED", 1990L));
        paymentService.handleWebhook(event("evt-1", "CAPTURED", 1990L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentId()).isEqualTo("pay-1");
    }

    @Test
    void rejectedPaymentDoesNotPayTheOrder() {
        paymentService.handleWebhook(event("evt-2", "REJECTED", 1990L));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.isPaid()).isFalse();
    }

    @Test
    void mismatchingAmountCannotPayTheOrder() {
        assertThatThrownBy(() -> paymentService.handleWebhook(event("evt-3", "CAPTURED", 1L)))
                .isInstanceOf(PaymentWebhookException.class);

        assertThat(order.isPaid()).isFalse();
    }

    @Test
    void notificationFromAnotherMerchantIsRejected() {
        WebhooksEvent foreign = event("evt-4", "CAPTURED", 1990L);
        foreign.setMerchantId("someone-else");

        assertThatThrownBy(() -> paymentService.handleWebhook(foreign))
                .isInstanceOf(PaymentWebhookException.class);

        assertThat(order.isPaid()).isFalse();
    }

    private WebhooksEvent event(String eventId, String status, long amountInCents) {
        WebhooksEvent event = new WebhooksEvent();
        event.setId(eventId);
        event.setMerchantId(PSPID);
        event.setType("payment.captured");
        event.setPayment(new PaymentResponse()
                .withId("pay-1")
                .withStatus(status)
                .withPaymentOutput(new PaymentOutput()
                        .withAmountOfMoney(new AmountOfMoney().withAmount(amountInCents).withCurrencyCode("EUR"))
                        .withReferences(new PaymentReferences().withMerchantReference(order.getReference()))));
        return event;
    }
}

