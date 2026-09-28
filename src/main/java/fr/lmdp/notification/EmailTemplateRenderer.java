package fr.lmdp.notification;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/**
 * Rendu des gabarits d'e-mail avec Thymeleaf, indépendamment du rendu des pages web.
 *
 * <p>Les gabarits vivent dans {@code templates/email/} (ex. {@code templates/email/commande-confirmation.html})
 * et sont traités avec le même moteur Thymeleaf que les vues, mais hors de tout contexte de requête HTTP :
 * toute URL utilisée dans un gabarit d'e-mail doit donc être absolue (ne pas utiliser {@code @{...}}).
 */
@Component
public class EmailTemplateRenderer {

    private final TemplateEngine templateEngine;

    public EmailTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /** @param template nom du gabarit sans le préfixe {@code email/} ni l'extension. */
    public String render(String template, Map<String, Object> variables) {
        Context context = new Context(Locale.FRANCE);
        context.setVariables(variables);
        return templateEngine.process("email/" + template, context);
    }
}

