package fr.lmdp;

import java.net.URI;
import com.onlinepayments.*;
import com.onlinepayments.authentication.AuthorizationType;
import com.onlinepayments.json.DefaultMarshaller;
import com.onlinepayments.merchant.MerchantClientInterface;
import com.onlinepayments.webhooks.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class CawlConfig {
//    @Bean(destroyMethod = "close")
//    ClientInterface cawlClient(@Value("${cawl.api-key}") String key,
//                              @Value("${cawl.api-secret}") String secret) {
//        return Factory.createClient(new CommunicatorConfiguration()
//            .withApiKeyId(key).withSecretApiKey(secret)
//            // Démonstration volontairement limitée à la préproduction.
//            .withApiEndpoint(URI.create("https://payment.preprod.cawl-solutions.fr/"))
//            .withIntegrator("spring-boot-demo").withAuthorizationType(AuthorizationType.V1HMAC));
//    }
//    @Bean
//    MerchantClientInterface merchant(ClientInterface client, @Value("${cawl.pspid}") String pspid) {
//        return client.merchant(pspid);
//    }
//    @Bean
//    WebhooksHelper webhooksHelper(@Value("${cawl.webhook-key}") String key,
//                                 @Value("${cawl.webhook-secret}") String secret) {
//        InMemorySecretKeyStore.INSTANCE.storeSecretKey(key, secret);
//        return new WebhooksHelper(DefaultMarshaller.INSTANCE, InMemorySecretKeyStore.INSTANCE);
//    }
}
