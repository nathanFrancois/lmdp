package fr.lmdp.catalog;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test
    void acceptsImageAtThreeMegabytesAndPersistsFeaturedFlag() {
        when(repository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        var image = new MockMultipartFile("image", "photo.png", "image/png", new byte[3 * 1024 * 1024]);
        var product = service.create("vase", "Vase", "Gravure", BigDecimal.TEN, 1, true, false, true, image);
        assertThat(product.getImageData()).hasSize(3 * 1024 * 1024);
        assertThat(product.isFeatured()).isTrue();
    }

    @Test
    void rejectsImageOverThreeMegabytesWithoutSaving() {
        var image = new MockMultipartFile("image", "photo.png", "image/png", new byte[3 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.create("vase", "Vase", "Gravure", BigDecimal.TEN, 1, true, false, false, image))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
        verify(repository, never()).save(any());
    }

    @Test
    void removesHighlightAndKeepsImageWhenUpdatingWithoutUpload() {
        var product = new Product("vase", "Vase", "Gravure", BigDecimal.TEN, 1, true, false);
        product.setFeatured(true);
        product.setImage(new byte[]{1, 2}, "image/png");
        when(repository.findById("vase")).thenReturn(Optional.of(product));
        service.update("vase", "Vase", "Gravure", BigDecimal.TEN, 1, true, false, false, null);
        assertThat(product.isFeatured()).isFalse();
        assertThat(product.getImageData()).containsExactly(1, 2);
    }

    @Test
    void customizableProductsCannotBeFeatured() {
        when(repository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));
        var product = service.create("vase", "Vase", "Gravure", BigDecimal.TEN, 1, true, true, true, null);
        assertThat(product.isFeatured()).isFalse();
    }
}
