package fr.lmdp.web;

import fr.lmdp.order.Order;
import fr.lmdp.order.OrderService;
import fr.lmdp.order.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Suivi des commandes dans le back-office : consultation et avancement du statut.
 * Protégé par {@code fr.lmdp.security.SecurityConfig} (rôle ADMIN).
 */
@Controller
@RequestMapping("/admin/commandes")
public class OrderAdminController {

    private final OrderService orderService;

    public OrderAdminController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderService.findAllRecentFirst());
        return "admin/commandes";
    }

    @GetMapping("/{reference}")
    public String detail(@PathVariable String reference, Model model) {
        model.addAttribute("order", requireOrder(reference));
        return "admin/commande-detail";
    }

    @PostMapping("/{reference}/statut")
    public String changeStatus(@PathVariable String reference, @RequestParam OrderStatus status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.changeStatus(reference, status);
            redirectAttributes.addFlashAttribute("message", "Statut mis à jour : " + status.getLabel() + ".");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Ce changement de statut n'est pas autorisé.");
        }
        return "redirect:/admin/commandes/" + reference;
    }

    private Order requireOrder(String reference) {
        return orderService.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
    }
}

