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

    @GetMapping("/{id}")
    public Product byId(@PathVariable String id) {
        return productService.findById(id)
                .filter(Product::isAvailable)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Création introuvable"));
    }
}

