package fr.lmdp.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * Identifiants d'accès à CAWL (plateforme Worldline). Aucune de ces valeurs n'est
 * versionnée : elles proviennent de variables d'environnement.
 *
 * @param pspid         identifiant du compte marchand (PSPID).
 * @param apiKey        clé d'API utilisée pour signer les appels sortants (V1HMAC).
 * @param apiSecret     secret associé à la clé d'API.
 * @param webhookKey    identifiant de clé utilisé pour vérifier la signature des webhooks.
 * @param webhookSecret secret associé à la clé de webhook.
 * @param apiEndpoint   URL de l'API (préproduction par défaut).
 * @param integrator    nom de l'intégrateur transmis à la plateforme.
 */
@ConfigurationProperties(prefix = "cawl")
public record CawlProperties(String pspid,
                             String apiKey,
                             String apiSecret,
                             String webhookKey,
                             String webhookSecret,
                             URI apiEndpoint,
                             String integrator) {

    public boolean isConfigured() {
        return StringUtils.hasText(pspid) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret);
    }
}

