package fr.lmdp.order;

import fr.lmdp.config.ShopProperties;
import fr.lmdp.notification.EmailSender;
import fr.lmdp.notification.EmailTemplateRenderer;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OrderMailServiceTest {
    @Test void reportsSuccessOnlyWhenSenderAcceptsMessage() {
        var sender = mock(EmailSender.class);
        when(sender.isEnabled()).thenReturn(true);
        var renderer = mock(EmailTemplateRenderer.class);
        when(renderer.render(anyString(), anyMap())).thenReturn("<p>Merci</p>");
        var service = new OrderMailService(sender, renderer, new ShopProperties("https://lmdp.net", BigDecimal.ZERO, ""));
        var order = Order.create("REF", new Customer("Claire", "Martin", "claire@example.com", null), DeliveryMethod.PICKUP, null, List.of(), BigDecimal.ZERO);
        assertThat(service.sendOrderConfirmation(order)).isTrue();
        doThrow(new IllegalStateException("Provider unavailable")).when(sender).send(anyString(), anyString(), anyString(), anyString(), anyString());
        assertThat(service.sendOrderConfirmation(order)).isFalse();
        reset(sender);
        when(sender.isEnabled()).thenReturn(false);
        assertThat(service.sendOrderConfirmation(order)).isFalse();
        verify(sender, never()).send(anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
