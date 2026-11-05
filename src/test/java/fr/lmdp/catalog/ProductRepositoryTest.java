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
