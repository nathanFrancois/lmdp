package fr.lmdp.web;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Back-office d'administration du catalogue (/admin). Accessible uniquement après
 * authentification (voir {@code fr.lmdp.security.SecurityConfig}) ; aucun
 * lien vers ces pages n'est exposé dans la navigation publique du site.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductService productService;

    public AdminController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/dashboard";
    }

    @GetMapping("/produits/nouveau")
    public String newProductForm(Model model) {
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductFormData(null, "", "", null, 0, true));
        }
        model.addAttribute("mode", "create");
        return "admin/produit-form";
    }

    @GetMapping("/produits/{id}/modifier")
    public String editProductForm(@PathVariable String id, Model model) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit introuvable"));
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductFormData(
                    product.getId(), product.getName(), product.getDescription(),
                    product.getPrice(), product.getStock(), product.isAvailable()));
        }
        model.addAttribute("productId", product.getId());
        model.addAttribute("imageUrl", product.getImageUrl());
        model.addAttribute("mode", "edit");
        return "admin/produit-form";
    }

    @PostMapping("/produits")
    public String create(@Valid @ModelAttribute("productForm") ProductFormData form, BindingResult binding,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("mode", "create");
            return "admin/produit-form";
        }
        productService.create(form.getId(), form.getName(), form.getDescription(), form.getPrice(),
                form.getStock(), form.isAvailable(), image);
        redirectAttributes.addFlashAttribute("message", "Produit créé avec succès.");
        return "redirect:/admin";
    }

    @PostMapping("/produits/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("productForm") ProductFormData form,
                          BindingResult binding, @RequestParam(value = "image", required = false) MultipartFile image,
                          Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            return editProductForm(id, model);
        }
        productService.update(id, form.getName(), form.getDescription(), form.getPrice(),
                form.getStock(), form.isAvailable(), image);
        redirectAttributes.addFlashAttribute("message", "Produit mis à jour avec succès.");
        return "redirect:/admin";
    }

    @PostMapping("/produits/{id}/supprimer")
    public String delete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Produit supprimé.");
        return "redirect:/admin";
    }
}



