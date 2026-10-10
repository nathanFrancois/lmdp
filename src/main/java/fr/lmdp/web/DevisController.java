package fr.lmdp.web;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import fr.lmdp.devis.DevisMailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Traitement du formulaire de demande de devis affiché sur la fiche d'un objet
 * de la page "Personnalisation" (voir {@code produit-personnalisation.html}).
 */
@Controller
public class DevisController {

    private static final Logger log = LoggerFactory.getLogger(DevisController.class);

    private final ProductService productService;
    private final DevisMailService devisMailService;

    public DevisController(ProductService productService, DevisMailService devisMailService) {
        this.productService = productService;
        this.devisMailService = devisMailService;
    }

    @PostMapping("/personnalisation/devis")
    public String submit(@Valid @ModelAttribute("devisForm") DevisForm form, BindingResult binding,
                         Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.findById(form.getProductId())
                .filter(Product::isAvailable)
                .filter(Product::isCustomizable)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Objet à personnaliser introuvable"));

        if (binding.hasErrors()) {
            model.addAttribute("activePage", "personnalisation");
            model.addAttribute("product", product);
            return "produit-personnalisation";
        }

        try {
            devisMailService.sendDevisRequest(product, form.getCustomerName(), form.getCustomerEmail(),
                    form.getCustomerPhone(), form.getMessage());
            redirectAttributes.addFlashAttribute("devisMessage",
                    "Votre demande de devis a bien été envoyée. Nous revenons vers vous rapidement !");
        } catch (RuntimeException e) {
            log.error("Objet {} : envoi de la demande de devis impossible", product.getId(), e);
            redirectAttributes.addFlashAttribute("devisError",
                    "Votre demande n'a pas pu être envoyée. Merci de réessayer dans quelques instants.");
        }
        return "redirect:/personnalisation/" + product.getId();
    }
}

