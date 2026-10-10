package fr.lmdp.web;

import fr.lmdp.devis.DevisMailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Pages "Votre objet" (gravure sur un objet apporté par le client) et
 * "Grands projets" (séries d'objets gravés pour un événement), chacune avec
 * son formulaire de demande de devis envoyé par e-mail.
 */
@Controller
public class ProjetController {

    private static final Logger log = LoggerFactory.getLogger(ProjetController.class);
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE);

    private static final String SUCCESS =
            "Merci ! Votre demande a bien été envoyée, nous revenons vers vous très vite.";
    private static final String ERROR =
            "Votre demande n'a pas pu être envoyée. Merci de réessayer dans quelques instants.";

    private final DevisMailService devisMailService;

    public ProjetController(DevisMailService devisMailService) {
        this.devisMailService = devisMailService;
    }

    // ---------- Votre objet ----------

    @GetMapping("/votre-objet")
    public String votreObjet(Model model) {
        if (!model.containsAttribute("projetForm")) {
            model.addAttribute("projetForm", new VotreObjetForm());
        }
        model.addAttribute("activePage", "votre-objet");
        return "votre-objet";
    }

    @PostMapping("/votre-objet/devis")
    public String votreObjetDevis(@Valid @ModelAttribute("projetForm") VotreObjetForm form, BindingResult binding,
                                  Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("activePage", "votre-objet");
            return "votre-objet";
        }
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Objet", form.getObjet());
        send("Votre objet", details, form, redirectAttributes);
        return "redirect:/votre-objet#demande";
    }

    // ---------- Grands projets ----------

    @GetMapping("/grands-projets")
    public String grandsProjets(Model model) {
        if (!model.containsAttribute("projetForm")) {
            model.addAttribute("projetForm", new GrandProjetForm());
        }
        model.addAttribute("activePage", "grands-projets");
        model.addAttribute("occasions", GrandProjetForm.OCCASIONS);
        return "grands-projets";
    }

    @PostMapping("/grands-projets/devis")
    public String grandsProjetsDevis(@Valid @ModelAttribute("projetForm") GrandProjetForm form, BindingResult binding,
                                     Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            model.addAttribute("activePage", "grands-projets");
            model.addAttribute("occasions", GrandProjetForm.OCCASIONS);
            return "grands-projets";
        }
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Occasion", form.getOccasion());
        details.put("Quantité", form.getQuantite() + " pièces");
        details.put("Date de l'événement",
                form.getDateEvenement() != null ? form.getDateEvenement().format(DATE_FR) : "Non précisée");
        send("Grand projet", details, form, redirectAttributes);
        return "redirect:/grands-projets#demande";
    }

    private void send(String label, Map<String, String> details, ProjetDevisForm form,
                      RedirectAttributes redirectAttributes) {
        try {
            devisMailService.sendProjectRequest(label, details, form.getCustomerName().trim(),
                    form.getCustomerEmail().trim(),
                    StringUtils.hasText(form.getCustomerPhone()) ? form.getCustomerPhone().trim() : null,
                    form.getMessage());
            redirectAttributes.addFlashAttribute("devisMessage", SUCCESS);
        } catch (RuntimeException e) {
            log.error("Demande de devis « {} » : envoi impossible", label, e);
            redirectAttributes.addFlashAttribute("devisError", ERROR);
        }
    }
}

