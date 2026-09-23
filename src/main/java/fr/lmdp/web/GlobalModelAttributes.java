package fr.lmdp.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Year;

/**
 * Attributs communs à toutes les pages (année courante pour le copyright).
 * Le panier n'est pas géré ici : il vit côté client (localStorage), voir /js/cart.js.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentYear")
    public int currentYear() {
        return Year.now().getValue();
    }
}


