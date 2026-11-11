package fr.lmdp.web;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API JSON en lecture seule, utilisée par le panier côté client (localStorage) pour
 * toujours récupérer le nom, le prix et la disponibilité à jour depuis le serveur,
 * sans jamais faire confiance à des données stockées dans le navigateur.
 */
@RestController
@RequestMapping("/api/produits")
public class ProductApiController {

    private final ProductService productService;

    public ProductApiController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> all() {
        return productService.findVisible();
    }

    @GetMapping("/panier")
    public List<fr.lmdp.catalog.CartProduct> cart(
            @org.springframework.web.bind.annotation.RequestParam(required = false) List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        if (ids.size() > 30 || ids.stream().anyMatch(id -> id.isBlank() || id.length() > 64)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Panier invalide");
        }
        return productService.findCartProducts(ids.stream().distinct().toList());
    }

    @GetMapping("/{id}")
    public Product byId(@PathVariable String id) {
        return productService.findById(id)
                .filter(Product::isAvailable)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Création introuvable"));
    }
}

