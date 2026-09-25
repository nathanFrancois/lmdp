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
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productService.findById("vase"))
                .thenReturn(Optional.of(new Product("vase", "Vase", "Un vase", new BigDecimal("19.90"), 5, true)));
        orderService = new OrderService(orderRepository, productService, new OrderReferenceGenerator(),
                new ShopProperties("http://localhost:8077", new BigDecimal("6.90")));
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
        assertThatThrownBy(() -> orderService.place(request(DeliveryMethod.PICKUP, "vase:9")))
                .isInstanceOf(CheckoutException.class);
    }

    @Test
    void confirmingPaymentTwiceDecreasesStockOnlyOnce() {
        Order order = orderService.place(request(DeliveryMethod.PICKUP, "vase:2"));

        orderService.confirmPayment(order, "pay-1", "CAPTURED");
        orderService.confirmPayment(order, "pay-1", "CAPTURED");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(productService, times(1)).decreaseStock(eq("vase"), anyInt());
    }

    private CheckoutRequest request(DeliveryMethod method, String cart) {
        return new CheckoutRequest(
                new Customer("Claire", "Martin", "claire@example.com", null),
                method,
                new ShippingAddress("1 rue des Fleurs", null, "75001", "Paris", "FR"),
                Cart.parse(cart));
    }
}

