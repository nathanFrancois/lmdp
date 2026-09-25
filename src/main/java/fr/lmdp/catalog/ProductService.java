package fr.lmdp.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
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

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Produits réellement visibles sur le site public (catalogue, fiches produit, API de lecture).
     * Un produit décoché "visible" dans l'administration n'apparaît plus du tout ici,
     * quel que soit son stock.
     */
    public List<Product> findVisible() {
        return productRepository.findByAvailableTrue();
    }

    public List<Product> findFeatured(int limit) {
        return findVisible().stream()
                .limit(limit)
                .toList();
    }

    public Optional<Product> findById(String id) {
        return productRepository.findById(id);
    }

    /**
     * Crée un nouveau produit. Si aucun identifiant n'est fourni, il est dérivé du nom.
     */
    @Transactional
    public Product create(String id, String name, String description, BigDecimal price, int stock,
                          boolean available, MultipartFile image) {
        String productId = (id == null || id.isBlank()) ? slugify(name) : id;
        if (productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un produit avec cet identifiant existe déjà.");
        }
        Product product = new Product(productId, name, description, price, stock, available);
        applyImage(product, image);
        return productRepository.save(product);
    }

    /**
     * Met à jour un produit existant. Lève une 404 si l'identifiant est inconnu.
     * Sans nouvelle image, la photo actuelle est conservée.
     */
    @Transactional
    public Product update(String id, String name, String description, BigDecimal price, int stock,
                          boolean available, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setAvailable(available);
        applyImage(product, image);
        return productRepository.save(product);
    }

    /**
     * Décrémente le stock après un paiement confirmé. Le stock ne peut pas devenir
     * négatif : en cas d'achats concurrents, la commande payée reste honorée et le
     * produit se retrouve simplement en rupture.
     */
    @Transactional
    public void decreaseStock(String productId, int quantity) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setStock(Math.max(0, product.getStock() - quantity));
            productRepository.save(product);
        });
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

    private static void applyImage(Product product, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }
        String extension = StringUtils.getFilenameExtension(image.getOriginalFilename());
        String contentType = switch (extension == null ? "" : extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format d'image non supporté (JPG, PNG, GIF, WEBP).");
        };
        try {
            // Le MIME est déterminé côté serveur, jamais repris d'un en-tête client arbitraire.
            product.setImage(image.getBytes(), contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire l'image envoyée.", e);
        }
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
