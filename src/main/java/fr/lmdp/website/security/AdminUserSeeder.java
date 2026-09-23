package fr.lmdp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Crée automatiquement un compte administrateur au premier démarrage si aucun
 * n'existe encore en base. Le mot de passe peut être imposé via les propriétés
 * {@code cawl.admin.username} / {@code cawl.admin.password} (ou les variables
 * d'environnement CAWL_ADMIN_USERNAME / CAWL_ADMIN_PASSWORD) ; à défaut, un mot
 * de passe aléatoire est généré et affiché une seule fois dans les logs.
 */
@Component
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final fr.lmdp.security.AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredUsername;
    private final String configuredPassword;

    public AdminUserSeeder(fr.lmdp.security.AdminUserRepository adminUserRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${cawl.admin.username:admin}") String configuredUsername,
                           @Value("${cawl.admin.password:}") String configuredPassword) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(String... args) {
        if (adminUserRepository.count() > 0) {
            return;
        }

        boolean generated = configuredPassword == null || configuredPassword.isBlank();
        String password = generated ? UUID.randomUUID().toString().substring(0, 12) : configuredPassword;

        adminUserRepository.save(new fr.lmdp.security.AdminUser(configuredUsername, passwordEncoder.encode(password)));

        if (generated) {
            log.warn("=================================================================");
            log.warn(" Aucun compte administrateur trouvé : un compte a été créé.");
            log.warn(" Identifiant : {}", configuredUsername);
            log.warn(" Mot de passe (temporaire, à changer après connexion) : {}", password);
            log.warn(" Connexion sur /admin/login");
            log.warn("=================================================================");
        }
    }
}

