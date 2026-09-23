package fr.lmdp.web;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contrôleurs de pages (vitrine). Aucune fonctionnalité de paiement, de compte
 * client ou d'administration n'est exposée ici.
 */
@Controller
public class PageController {

    private final ProductService productService;

    public PageController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activePage", "accueil");
        return "home";
    }

    @GetMapping("/atelier")
    public String atelier(Model model) {
        model.addAttribute("activePage", "atelier");
        return "atelier";
    }

    @GetMapping("/creations")
    public String creations(Model model) {
        model.addAttribute("activePage", "creations");
        model.addAttribute("products", productService.findVisible());
        return "creations";
    }

    @GetMapping("/creations/{id}")
    public String productDetail(@PathVariable String id, Model model) {
        Product product = productService.findById(id)
                .filter(Product::isAvailable)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Création introuvable"));
        model.addAttribute("activePage", "creations");
        model.addAttribute("product", product);
        return "produit";
    }

    @GetMapping("/panier")
    public String cart(Model model) {
        model.addAttribute("activePage", "panier");
        return "panier";
    }

    @GetMapping("/commande")
    public String checkout(Model model) {
        model.addAttribute("activePage", "panier");
        return "commande";
    }
}


