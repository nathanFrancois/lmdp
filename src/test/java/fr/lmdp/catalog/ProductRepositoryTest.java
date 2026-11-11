package fr.lmdp.catalog;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ProductRepositoryTest {
    @Autowired private ProductRepository repository;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    @Test
    void cartProjectionExcludesUnrequestedHiddenAndCustomizableProducts() {
        var visible = new Product("wanted", "Vase", "Description", BigDecimal.TEN, 3, true, false);
        visible.setImage(new byte[3 * 1024 * 1024], "image/png");
        repository.saveAll(java.util.List.of(visible,
                new Product("hidden", "Hidden", "Description", BigDecimal.TEN, 1, false, false),
                new Product("custom", "Custom", "Description", BigDecimal.TEN, 1, true, true),
                new Product("other", "Other", "Description", BigDecimal.TEN, 1, true, false)));
        repository.flush();
        entityManager.clear();
        var result = repository.findCartProducts(java.util.List.of("wanted", "hidden", "custom"));
        assertThat(result).extracting(CartProduct::id).containsExactly("wanted");
        assertThat(result.get(0).getImageUrl()).isEqualTo("/api/produits/wanted/image");
    }

    @Test
    void selectingAnotherProductReplacesPreviousSelection() {
        var service = new ProductService(repository);
        service.create("first", "Premier", "Gravure", BigDecimal.TEN, 1, true, false, true, null);
        service.create("second", "Second", "Gravure", BigDecimal.TEN, 1, true, false, true, null);
        repository.flush();
        entityManager.clear();
        assertThat(repository.findByFeaturedTrue()).extracting(Product::getId).containsExactly("second");
        service.update("first", "Premier", "Gravure", BigDecimal.TEN, 1, true, false, true, null);
        repository.flush();
        entityManager.clear();
        assertThat(repository.findByFeaturedTrue()).extracting(Product::getId).containsExactly("first");
        service.update("first", "Premier", "Gravure", BigDecimal.TEN, 1, true, false, false, null);
        repository.flush();
        assertThat(repository.findByFeaturedTrue()).isEmpty();
    }

    @Test
    void migrationPersistsHighlightAndVisibilityFiltersStillApply() {
        var visible = new Product("visible", "Vase", "Gravure", BigDecimal.TEN, 1, true, false);
        visible.setFeatured(true);
        var hidden = new Product("hidden", "Verre", "Gravure", BigDecimal.TEN, 1, false, false);
        hidden.setFeatured(true);
        var custom = new Product("custom", "Verre", "Gravure", BigDecimal.TEN, 1, true, true);
        repository.saveAll(java.util.List.of(visible, hidden, custom));
        repository.flush();
        entityManager.clear();
        assertThat(repository.findByAvailableTrueAndCustomizableFalse())
                .extracting(Product::getId).containsExactly("visible");
        assertThat(repository.findById("visible").orElseThrow().isFeatured()).isTrue();
        assertThat(repository.findById("custom").orElseThrow().isFeatured()).isFalse();
    }
}
