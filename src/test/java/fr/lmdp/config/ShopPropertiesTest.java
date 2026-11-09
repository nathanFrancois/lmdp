package fr.lmdp.config;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ShopPropertiesTest {
    @Test void publicDomainReplacesLegacyOrigin() {
        for (String origin : new String[]{null, "", "https://lmdp.onrender.com/", "https://lmdp.net/"}) {
            assertThat(new ShopProperties(origin, BigDecimal.ZERO, "").url("/commande/ABC/retour"))
                    .isEqualTo("https://lmdp.net/commande/ABC/retour");
        }
    }
    @Test void developmentOriginRemainsConfigurable() {
        assertThat(new ShopProperties("http://localhost:8080/", BigDecimal.ZERO, "").url("/commande/ABC"))
                .isEqualTo("http://localhost:8080/commande/ABC");
    }
}
