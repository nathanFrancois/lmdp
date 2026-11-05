package fr.lmdp.web;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PageControllerTest {
    @Test
    void withoutFeaturedProductTheCatalogueRemainsComplete() {
        var service = mock(ProductService.class);
        var regular = new Product("regular", "Vase", "Gravure", BigDecimal.TEN, 1, true, false);
        when(service.findVisible()).thenReturn(List.of(regular));
        var model = new ExtendedModelMap();
        new PageController(service).creations(model);
        assertThat(model.get("featuredProduct")).isNull();
        assertThat(model.get("products")).isEqualTo(List.of(regular));
    }

    @Test
    void creationsDisplayFeaturedProductsFirstWithoutDuplicates() {
        var service = mock(ProductService.class);
        var regular = new Product("regular", "Vase", "Gravure", BigDecimal.TEN, 1, true, false);
        var featured = new Product("featured", "Verre", "Gravure", BigDecimal.TEN, 0, true, false);
        featured.setFeatured(true);
        when(service.findVisible()).thenReturn(List.of(regular, featured));
        var model = new ExtendedModelMap();
        assertThat(new PageController(service).creations(model)).isEqualTo("creations");
        assertThat(model.get("featuredProduct")).isEqualTo(featured);
        assertThat(model.get("products")).isEqualTo(List.of(regular));
    }
}
