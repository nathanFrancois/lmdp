package fr.lmdp.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Accès au catalogue, adossé à la base de données (voir {@link ProductRepository}).
 * Le schéma est créé et versionné par Flyway ; ce service ne fait qu'orchestrer
 * les opérations de lecture et d'écriture.
 */
@Service
public class ProductService {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private final fr.lmdp.catalog.ProductRepository productRepository;

    public ProductService(fr.lmdp.catalog.ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<fr.lmdp.catalog.Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Produits réellement visibles sur le site public (catalogue, fiches produit, API de lecture).
     * Un produit décoché "visible" dans l'administration n'apparaît plus du tout ici,
     * quel que soit son stock.
     */
    public List<fr.lmdp.catalog.Product> findVisible() {
        return productRepository.findByAvailableTrue();
    }

    public List<fr.lmdp.catalog.Product> findFeatured(int limit) {
        return findVisible().stream()
                .limit(limit)
                .toList();
    }

    public Optional<fr.lmdp.catalog.Product> findById(String id) {
        return productRepository.findById(id);
    }

    /**
     * Crée un nouveau produit. Si aucun identifiant n'est fourni, il est dérivé du nom.
     */
    public fr.lmdp.catalog.Product create(String id, String name, String description, BigDecimal price, int stock,
                                          boolean available, String imageFilename) {
        String productId = (id == null || id.isBlank()) ? slugify(name) : id;
        if (productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un produit avec cet identifiant existe déjà.");
        }
        fr.lmdp.catalog.Product product = new fr.lmdp.catalog.Product(productId, name, description, price, stock, available);
        product.setImageFilename(imageFilename);
        return productRepository.save(product);
    }

    /**
     * Met à jour un produit existant. Lève une 404 si l'identifiant est inconnu.
     * {@code newImageFilename} ne remplace la photo existante que s'il est non nul
     * (aucune nouvelle photo envoyée = la photo actuelle est conservée).
     */
    public fr.lmdp.catalog.Product update(String id, String name, String description, BigDecimal price, int stock,
                                          boolean available, String newImageFilename) {
        fr.lmdp.catalog.Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setAvailable(available);
        if (newImageFilename != null) {
            product.setImageFilename(newImageFilename);
        }
        return productRepository.save(product);
    }

    /**
     * Supprime un produit. Lève une 404 si l'identifiant est inconnu.
     */
    public void delete(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable");
        }
        productRepository.deleteById(id);
    }

    private static String slugify(String name) {
        String normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-").replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "produit-" + System.currentTimeMillis();
        }
        return slug;
    }
}
