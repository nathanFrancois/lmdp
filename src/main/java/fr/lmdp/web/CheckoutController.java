package fr.lmdp.web;

import fr.lmdp.config.ShopProperties;
import fr.lmdp.order.CheckoutException;
import fr.lmdp.order.Order;
import fr.lmdp.order.OrderService;
import fr.lmdp.payment.PaymentException;
import fr.lmdp.payment.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tunnel de commande : saisie des coordonnées et du mode de remise, redirection vers
 * la page de paiement CAWL, puis page de suivi.
 *
 * <p>Le navigateur ne transmet que des identifiants de produit et des quantités ; le
 * détail, les prix et le total sont recalculés ici à partir du catalogue.
 */
@Controller
@RequestMapping("/commande")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ShopProperties shopProperties;

    public CheckoutController(OrderService orderService, PaymentService paymentService,
                              ShopProperties shopProperties) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.shopProperties = shopProperties;
    }

    @GetMapping
    public String form(Model model) {
        if (!model.containsAttribute("checkoutForm")) {
            model.addAttribute("checkoutForm", new CheckoutForm());
        }
        return prepareForm(model);
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("checkoutForm") CheckoutForm form, BindingResult binding,
                         Model model) {
        validateAddress(form, binding);
        if (binding.hasErrors()) {
            return prepareForm(model);
        }
        try {
            Order order = orderService.place(form.toCheckoutRequest());
            String paymentUrl = paymentService.startPayment(order);
            return "redirect:" + paymentUrl;
        } catch (CheckoutException e) {
            model.addAttribute("checkoutError", e.getMessage());
        } catch (PaymentException e) {
            log.error("Ouverture du paiement impossible", e);
            model.addAttribute("checkoutError",
                    "Le paiement en ligne est momentanément indisponible. Merci de réessayer plus tard.");
        }
        return prepareForm(model);
    }

    /** Page de retour depuis CAWL : on relit le statut auprès de la plateforme, jamais du navigateur. */
    @GetMapping("/{reference}/retour")
    public String paymentReturn(@PathVariable String reference, Model model) {
        Order order = requireOrder(reference);
        paymentService.refreshPaymentStatus(order);
        return confirmation(reference, model);
    }

    /** Page de suivi, accessible avec la référence de commande. */
    @GetMapping("/{reference}")
    public String confirmation(@PathVariable String reference, Model model) {
        model.addAttribute("activePage", "panier");
        model.addAttribute("order", requireOrder(reference));
        return "commande-confirmation";
    }

    private Order requireOrder(String reference) {
        return orderService.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
    }

    private String prepareForm(Model model) {
        model.addAttribute("activePage", "panier");
        model.addAttribute("shippingFee", shopProperties.shippingFee());
        return "commande";
    }

    /** L'adresse n'est exigée que pour une livraison : la remise en main propre s'en passe. */
    private void validateAddress(CheckoutForm form, BindingResult binding) {
        if (!form.needsAddress()) {
            return;
        }
        requireField(binding, "street", form.getStreet(), "L'adresse est obligatoire pour une livraison.");
        requireField(binding, "postalCode", form.getPostalCode(), "Le code postal est obligatoire.");
        requireField(binding, "city", form.getCity(), "La ville est obligatoire.");
    }

    private void requireField(BindingResult binding, String field, String value, String message) {
        if (!StringUtils.hasText(value)) {
            binding.rejectValue(field, "required", message);
        }
    }
}

