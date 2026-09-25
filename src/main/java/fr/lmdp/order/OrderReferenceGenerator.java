package fr.lmdp.order;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Génère la référence publique d'une commande. Elle est aléatoire (non séquentielle)
 * pour empêcher l'énumération des commandes via l'URL de suivi, et suffisamment courte
 * pour être acceptée comme référence marchande par CAWL (30 caractères maximum).
 */
@Component
public class OrderReferenceGenerator {

    private static final String PREFIX = "LMDP-";
    private static final int RANDOM_BYTES = 8;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return PREFIX + HexFormat.of().withUpperCase().formatHex(bytes);
    }
}

