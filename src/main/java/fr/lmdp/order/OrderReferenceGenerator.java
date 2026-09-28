package fr.lmdp.order;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Génère la référence publique d'une commande. Elle est aléatoire (non séquentielle),
 * générée par un générateur cryptographique ({@link SecureRandom}), pour empêcher
 * l'énumération des commandes via l'URL de suivi (page de suivi, lien envoyé par e-mail),
 * et suffisamment courte pour être acceptée comme référence marchande par CAWL
 * (30 caractères maximum : "LMDP-" + 24 caractères hexadécimaux = 29).
 *
 * <p>12 octets aléatoires = 96 bits d'entropie, soit 2^96 (≈ 7,9 × 10^28) valeurs
 * possibles : une énumération par force brute est totalement hors de portée.
 */
@Component
public class OrderReferenceGenerator {

    private static final String PREFIX = "LMDP-";
    private static final int RANDOM_BYTES = 12;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        random.nextBytes(bytes);
        return PREFIX + HexFormat.of().withUpperCase().formatHex(bytes);
    }
}

