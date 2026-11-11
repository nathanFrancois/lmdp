package fr.lmdp.web;

import fr.lmdp.catalog.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProductApiController.class, ProductImageController.class}, properties = {"spring.thymeleaf.cache=false", "server.address=127.0.0.1", "server.port=0"})
@AutoConfigureMockMvc(addFilters = false)
class ProductPerformanceTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductService products;

    @Test void cartSelectsOnlyRequestedProductsAndReturnsImageUrl() throws Exception {
        when(products.findCartProducts(List.of("vase"))).thenReturn(List.of(new CartProduct("vase", "Vase", BigDecimal.TEN, 3, true)));
        mvc.perform(get("/api/produits/panier").param("ids", "vase")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].imageUrl").value("/api/produits/vase/image"))
                .andExpect(content().string(not(containsString("base64"))));
        verify(products).findCartProducts(List.of("vase"));
        verify(products, never()).findVisible();
    }
    @Test void emptyCartDoesNotReadCatalogue() throws Exception {
        mvc.perform(get("/api/produits/panier")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verifyNoInteractions(products);
    }
    @Test void imageRevalidatesAndChangesInvalidateCache() throws Exception {
        var p = new Product("vase", "Vase", "Description", BigDecimal.TEN, 1, true, false);
        p.setImage(new byte[]{1, 2, 3}, "image/png");
        when(products.findById("vase")).thenReturn(Optional.of(p));
        var response = mvc.perform(get("/api/produits/vase/image")).andExpect(status().isOk())
                .andExpect(content().contentType("image/png")).andReturn().getResponse();
        String etag = response.getHeader("ETag");
        mvc.perform(get("/api/produits/vase/image").header("If-None-Match", etag)).andExpect(status().isNotModified());
        p.setImage(new byte[]{4, 5}, "image/png");
        mvc.perform(get("/api/produits/vase/image").header("If-None-Match", etag)).andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{4, 5}));
        p.setAvailable(false);
        mvc.perform(get("/api/produits/vase/image").header("If-None-Match", etag)).andExpect(status().isNotFound());
    }
}
