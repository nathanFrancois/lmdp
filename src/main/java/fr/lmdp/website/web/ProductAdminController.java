package fr.lmdp.web;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductRequest;
import fr.lmdp.catalog.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API d'administration du catalogue : création, modification et suppression des produits.
 * Contrairement à {@link ProductApiController} (lecture seule, utilisée par le panier public),
 * ces opérations d'écriture sont destinées à un usage interne / back-office et devraient être
 * protégées (authentification) avant toute exposition en production.
 */
@RestController
@RequestMapping("/api/admin/produits")
public class ProductAdminController {

    private final ProductService productService;

    public ProductAdminController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product created = productService.create(
                request.id(), request.name(), request.description(), request.price(), request.stock(),
                request.available(), null);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request.name(), request.description(), request.price(), request.stock(),
                request.available(), null);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

