package fr.lmdp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.onlinepayments.domain.*;
import com.onlinepayments.merchant.MerchantClientInterface;
import com.onlinepayments.json.DefaultMarshaller;
import com.onlinepayments.webhooks.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentTest {
    MerchantClientInterface merchant;
    PaymentService service;
    String reference;
    @BeforeEach void init() {
        merchant = mock(MerchantClientInterface.class, RETURNS_DEEP_STUBS);
        when(merchant.hostedCheckout().createHostedCheckout(any(CreateHostedCheckoutRequest.class)))
            .thenReturn(new CreateHostedCheckoutResponse().withHostedCheckoutId("hc-1")
                .withRedirectUrl("https://payment.preprod.cawl-solutions.fr/example"));
        service = new PaymentService(merchant, "merchant-test", "http://localhost:8080/payment-return");
        service.createCheckout("demo-001");
        var captor = ArgumentCaptor.forClass(CreateHostedCheckoutRequest.class);
        verify(merchant.hostedCheckout()).createHostedCheckout(captor.capture());
        reference = captor.getValue().getOrder().getReferences().getMerchantReference();
        assertThat(captor.getValue().getOrder().getAmountOfMoney().getAmount()).isEqualTo(1990L);
    }
    WebhooksEvent event(String id, String status, long amount) {
        var event = new WebhooksEvent();
        event.setId(id); event.setMerchantId("merchant-test"); event.setType("payment.captured");
        event.setPayment(new PaymentResponse().withId("pay-1").withStatus(status)
            .withPaymentOutput(new PaymentOutput()
                .withAmountOfMoney(new AmountOfMoney().withAmount(amount).withCurrencyCode("EUR"))
                .withReferences(new PaymentReferences().withMerchantReference(reference))));
        return event;
    }
    @Test void repeatedCheckoutReusesSession() {
        assertThat(service.createCheckout("demo-001").hostedCheckoutId()).isEqualTo("hc-1");
        verify(merchant.hostedCheckout(), times(1)).createHostedCheckout(any(CreateHostedCheckoutRequest.class));
    }
    @Test void captureIsIdempotentAndCannotBeDowngraded() {
        var captured = event("e-1", "CAPTURED", 1990);
        service.accept(captured); service.accept(captured);
        service.accept(event("e-2", "AUTHORIZATION_REQUESTED", 1990));
        assertThat(service.state("demo-001").status()).isEqualTo("PAID");
    }
    @Test void authorizationAloneIsNotPaid() {
        service.accept(event("e-1", "PENDING_CAPTURE", 1990));
        assertThat(service.state("demo-001").status()).isNotEqualTo("PAID");
    }
    @Test void wrongAmountCannotPayOrder() {
        assertThatThrownBy(() -> service.accept(event("e-1", "CAPTURED", 1)))
            .isInstanceOf(ResponseStatusException.class);
        assertThat(service.state("demo-001").status()).isEqualTo("UNPAID");
    }
    @Test void wrongMerchantCannotPayOrder() {
        var event = event("e-1", "CAPTURED", 1990); event.setMerchantId("other");
        assertThatThrownBy(() -> service.accept(event)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void verifiesSignatureOnExactBytes() throws Exception {
        String secret = "local-test-secret";
        InMemorySecretKeyStore.INSTANCE.storeSecretKey("test-key", secret);
        var controller = new PaymentController(service,
            new WebhooksHelper(DefaultMarshaller.INSTANCE, InMemorySecretKeyStore.INSTANCE));
        byte[] body = "{\"apiVersion\":\"v1\",\"id\":\"test-event\",\"merchantId\":\"merchant-test\",\"type\":\"payment.test\"}"
            .getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var headers = new HttpHeaders();
        headers.add("X-GCS-KeyId", "test-key");
        headers.add("X-GCS-Signature", Base64.getEncoder().encodeToString(mac.doFinal(body)));
        assertThat(controller.webhook(body, headers).getStatusCode().value()).isEqualTo(204);
        byte[] changed = (new String(body, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> controller.webhook(changed, headers))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(401));
    }
}
