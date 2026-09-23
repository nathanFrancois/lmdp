package fr.lmdp.web;

import fr.lmdp.security.AdminUser;

import fr.lmdp.security.AdminUserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Permet à l'administrateur connecté de changer son propre mot de passe
 * (utile après la connexion avec le mot de passe généré automatiquement au
 * premier démarrage, voir {@code AdminUserSeeder}).
 */
@Controller
@RequestMapping("/admin/mot-de-passe")
public class AdminAccountController {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountController(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String form(Model model) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm("", "", ""));
        }
        return "admin/mot-de-passe";
    }

    @PostMapping
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                  org.springframework.validation.BindingResult binding,
                                  Authentication authentication, Model model,
                                  RedirectAttributes redirectAttributes) {
        AdminUser user = adminUserRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable"));

        if (!binding.hasFieldErrors("currentPassword")
                && !passwordEncoder.matches(form.currentPassword(), user.getPasswordHash())) {
            binding.rejectValue("currentPassword", "invalid", "Mot de passe actuel incorrect.");
        }
        if (!binding.hasFieldErrors("newPassword") && !binding.hasFieldErrors("confirmPassword")
                && !form.newPassword().equals(form.confirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "La confirmation ne correspond pas au nouveau mot de passe.");
        }

        if (binding.hasErrors()) {
            return "admin/mot-de-passe";
        }

        user.setPasswordHash(passwordEncoder.encode(form.newPassword()));
        adminUserRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "Mot de passe mis à jour avec succès.");
        return "redirect:/admin";
    }

    public record PasswordChangeForm(
            @NotBlank(message = "Le mot de passe actuel est obligatoire") String currentPassword,
            @NotBlank(message = "Le nouveau mot de passe est obligatoire")
            @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères") String newPassword,
            @NotBlank(message = "La confirmation est obligatoire") String confirmPassword) {
    }
}

