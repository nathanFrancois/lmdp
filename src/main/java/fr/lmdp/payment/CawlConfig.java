package fr.lmdp.payment;

import com.onlinepayments.ClientInterface;
import com.onlinepayments.CommunicatorConfiguration;
import com.onlinepayments.Factory;
import com.onlinepayments.authentication.AuthorizationType;
import com.onlinepayments.json.DefaultMarshaller;
import com.onlinepayments.merchant.MerchantClientInterface;
import com.onlinepayments.webhooks.InMemorySecretKeyStore;
import com.onlinepayments.webhooks.WebhooksHelper;
import fr.lmdp.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Câblage du SDK CAWL. Si les identifiants ne sont pas fournis (poste de
 * développement, environnement de test), une passerelle de repli est utilisée :
 * le site démarre normalement et seul le paiement est annoncé comme indisponible.
 */
@Configuration
public class CawlConfig {

    private static final Logger log = LoggerFactory.getLogger(CawlConfig.class);

    // Beans paresseux : sans identifiants, aucun client n'est instancié et le site démarre quand même.
    @Bean(destroyMethod = "close")
    @Lazy
    ClientInterface cawlClient(CawlProperties properties) {
        return Factory.createClient(new CommunicatorConfiguration()
                .withApiKeyId(properties.apiKey())
                .withSecretApiKey(properties.apiSecret())
                .withApiEndpoint(properties.apiEndpoint())
                .withIntegrator(properties.integrator())
                .withAuthorizationType(AuthorizationType.V1HMAC));
    }

    @Bean
    @Lazy
    MerchantClientInterface cawlMerchantClient(ClientInterface client, CawlProperties properties) {
        return client.merchant(properties.pspid());
    }

    @Bean
    PaymentGateway paymentGateway(ObjectProvider<MerchantClientInterface> merchantClient, CawlProperties properties) {
        if (!properties.isConfigured()) {
            log.warn("Identifiants CAWL absents : le paiement en ligne est désactivé sur cet environnement.");
            return new UnavailablePaymentGateway();
        }
        return new CawlPaymentGateway(merchantClient.getObject());
    }

    /**
     * Vérificateur de signature des notifications CAWL : sans lui, n'importe qui
     * pourrait déclarer une commande payée.
     */
    @Bean
    WebhooksHelper cawlWebhooksHelper(CawlProperties properties) {
        if (StringUtils.hasText(properties.webhookKey()) && StringUtils.hasText(properties.webhookSecret())) {
            InMemorySecretKeyStore.INSTANCE.storeSecretKey(properties.webhookKey(), properties.webhookSecret());
        } else {
            log.warn("Clé de webhook CAWL absente : les notifications de paiement seront toutes rejetées.");
        }
        return new WebhooksHelper(DefaultMarshaller.INSTANCE, InMemorySecretKeyStore.INSTANCE);
    }

    /** Passerelle de repli, utilisée tant que la boutique n'a pas ses identifiants CAWL. */
    private static final class UnavailablePaymentGateway implements PaymentGateway {

        @Override
        public HostedCheckoutSession openCheckout(Order order, String returnUrl) {
            throw new PaymentException("Le paiement en ligne n'est pas configuré sur cet environnement.");
        }

        @Override
        public Optional<PaymentOutcome> readOutcome(String hostedCheckoutId) {
            return Optional.empty();
        }
    }
}





