package fr.lmdp.web;

import fr.lmdp.config.ShopProperties;
import fr.lmdp.order.*;
import fr.lmdp.payment.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CheckoutController.class, properties = {"spring.thymeleaf.cache=false", "server.address=127.0.0.1", "server.port=0"})
@AutoConfigureMockMvc(addFilters = false)
class CheckoutConfirmationTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService orders;
    @MockitoBean PaymentService payments;
    @org.springframework.boot.test.context.TestConfiguration
    static class Config {
        @org.springframework.context.annotation.Bean
        ShopProperties shopProperties() { return new ShopProperties("https://lmdp.net", BigDecimal.ZERO, ""); }
    }
    private Order order() {
        var order = Order.create("REF", new Customer("Claire", "Martin", "claire@example.com", null), DeliveryMethod.PICKUP, null, List.of(OrderLine.of(new fr.lmdp.catalog.Product("vase", "Vase fleuri", "Gravure artisanale", new BigDecimal("29.90"), 1, true, false), 1)), BigDecimal.ZERO);
        when(orders.findByReference("REF")).thenReturn(Optional.of(order));
        return order;
    }
    @Test void paidAndEmailSent() throws Exception {
        var order = order(); order.markPaid("p", "CAPTURED"); order.markConfirmationEmailSent();
        mvc.perform(get("/commande/REF")).andExpect(status().isOk())
            .andDo(result -> java.nio.file.Files.writeString(java.nio.file.Path.of("target/confirmation-paid.html"), result.getResponse().getContentAsString()))
            .andExpect(content().string(containsString("Un e-mail de confirmation a été envoyé")))
            .andExpect(content().string(containsString("data-clear-cart=\"true\"")));
    }
    @Test void paidWithoutEmailDoesNotClaimItWasSent() throws Exception {
        order().markPaid("p", "CAPTURED");
        mvc.perform(get("/commande/REF")).andExpect(status().isOk())
            .andExpect(content().string(containsString("l’envoi de l’e-mail n’est pas confirmé")))
            .andExpect(content().string(not(containsString("Un e-mail de confirmation a été envoyé"))));
    }
    @Test void pendingProvidesSafeStatusRefresh() throws Exception {
        order();
        mvc.perform(get("/commande/REF/retour")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Confirmation en cours")))
            .andExpect(content().string(containsString("Vérifier mon paiement")))
            .andExpect(content().string(containsString("data-clear-cart=\"false\"")));
        verify(payments).refreshPaymentStatus(any(Order.class));
    }
    @Test void failedProvidesCartLink() throws Exception {
        order().markPaymentFailed("REJECTED");
        mvc.perform(get("/commande/REF")).andExpect(status().isOk())
            .andDo(result -> java.nio.file.Files.writeString(java.nio.file.Path.of("target/confirmation-failed.html"), result.getResponse().getContentAsString()))
            .andExpect(content().string(containsString("Le paiement n’a pas abouti")))
            .andExpect(content().string(containsString("Revenir au panier")));
    }
    @Test void cancelledPaidOrderDoesNotDisplayThankYou() throws Exception {
        var order = order(); order.markPaid("p", "CAPTURED"); order.changeStatus(OrderStatus.CANCELLED);
        mvc.perform(get("/commande/REF")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Votre commande est annulée")))
            .andExpect(content().string(not(containsString("Merci pour votre commande !"))));
    }
}
