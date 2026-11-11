package fr.lmdp.web;

import fr.lmdp.catalog.ProductService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Controller
public class ProductImageController {
    private final ProductService service;
    public ProductImageController(ProductService service) { this.service = service; }

    @GetMapping("/api/produits/{id}/image")
    public ResponseEntity<byte[]> publicImage(@PathVariable String id, WebRequest request) {
        return image(id, request, false);
    }

    // Protégé par la règle ADMIN existante ; aperçu des produits masqués.
    @GetMapping("/api/admin/produits/{id}/image")
    public ResponseEntity<byte[]> adminImage(@PathVariable String id, WebRequest request) {
        return image(id, request, true);
    }

    private ResponseEntity<byte[]> image(String id, WebRequest request, boolean admin) {
        var product = service.findById(id).filter(p -> admin || p.isAvailable())
                .filter(p -> p.isHasImage())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String etag;
        try {
            etag = "\"" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(product.getImageData())) + "\"";
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        var headers = new HttpHeaders();
        headers.setETag(etag);
        // Revalider pour respecter les changements de photo et de visibilité.
        headers.setCacheControl("private, no-cache");
        if (request.checkNotModified(etag)) return new ResponseEntity<>(null, headers, HttpStatus.NOT_MODIFIED);
        headers.setContentType(MediaType.parseMediaType(product.getImageContentType()));
        return new ResponseEntity<>(product.getImageData(), headers, HttpStatus.OK);
    }
}
