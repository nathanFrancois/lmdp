package fr.lmdp.order;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import fr.lmdp.config.ShopProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ProductService productService = mock(ProductService.class);
    private final OrderMailService orderMailService = mock(OrderMailService.class);
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productService.findById("vase"))
                .thenReturn(Optional.of(new Product("vase", "Vase", "Un vase", new BigDecimal("19.90"), 20, true, false)));
        orderService = new OrderService(orderRepository, productService, new OrderReferenceGenerator(),
                new ShopProperties("http://localhost:8077", new BigDecimal("6.90"), ""), orderMailService);
    }

    @Test
    void computesTotalFromCatalogAndAddsShippingOnlyForDelivery() {
        Order pickup = orderService.place(request(DeliveryMethod.PICKUP, "vase:2"));
        Order shipped = orderService.place(request(DeliveryMethod.SHIPPING, "vase:2"));

        assertThat(pickup.getTotalAmount()).isEqualByComparingTo("39.80");
        assertThat(shipped.getTotalAmount()).isEqualByComparingTo("46.70");
        assertThat(pickup.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
    }

    @Test
    void refusesOrderWhenStockIsInsufficient() {
        assertThatThrownBy(() -> orderService.place(request(DeliveryMethod.PICKUP, "vase:99")))
                .isInstanceOf(CheckoutException.class);
    }

    @Test
    void confirmingPaymentTwiceDecreasesStockOnlyOnce() {
        Order order = orderService.place(request(DeliveryMethod.PICKUP, "vase:2"));

        when(orderMailService.sendOrderConfirmation(order)).thenReturn(true);
        orderService.confirmPayment(order, "pay-1", "CAPTURED");
        orderService.confirmPayment(order, "pay-1", "CAPTURED");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.isConfirmationEmailSent()).isTrue();
        verify(productService, times(1)).decreaseStock(eq("vase"), anyInt());
        verify(orderMailService, times(1)).sendOrderConfirmation(order);
        verify(orderMailService, times(1)).sendSellerNotification(order);
    }

    @Test
    void shippingCountsAllUnitsAtEachBoundary() {
        int[] quantities = {1, 3, 4, 5, 6, 12};
        String[] fees = {"6.90", "6.90", "7.90", "7.90", "9.90", "9.90"};
        for (int i = 0; i < quantities.length; i++) {
            var order = orderService.place(request(DeliveryMethod.SHIPPING, "vase:" + quantities[i]));
            assertThat(order.getShippingFee()).isEqualByComparingTo(fees[i]);
            assertThat(order.getTotalAmount()).isEqualByComparingTo(
                    new BigDecimal("19.90").multiply(BigDecimal.valueOf(quantities[i])).add(new BigDecimal(fees[i])));
            assertThat(orderService.place(request(DeliveryMethod.PICKUP, "vase:" + quantities[i])).getShippingFee()).isZero();
        }
    }

    @Test
    void failedCustomerEmailStillNotifiesSeller() {
        var order = orderService.place(request(DeliveryMethod.PICKUP, "vase:1"));
        when(orderMailService.sendOrderConfirmation(order)).thenReturn(false);
        orderService.confirmPayment(order, "pay-1", "CAPTURED");
        verify(orderMailService).sendSellerNotification(order);
    }

    @Test
    void unpaidOrdersDoNotNotifySeller() {
        var order = orderService.place(request(DeliveryMethod.PICKUP, "vase:1"));
        orderService.rejectPayment(order, "REJECTED");
        verify(orderMailService, org.mockito.Mockito.never()).sendSellerNotification(any());
    }

    private CheckoutRequest request(DeliveryMethod method, String cart) {
        return new CheckoutRequest(
                new Customer("Claire", "Martin", "claire@example.com", null),
                method,
                new ShippingAddress("1 rue des Fleurs", null, "75001", "Paris", "FR"),
                Cart.parse(cart));
    }
}

