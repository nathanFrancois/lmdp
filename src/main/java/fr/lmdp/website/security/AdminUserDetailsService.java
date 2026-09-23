package fr.lmdp.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Charge les administrateurs depuis la base de données ({@link AdminUserRepository})
 * pour l'authentification du back-office /admin.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final fr.lmdp.security.AdminUserRepository adminUserRepository;

    public AdminUserDetailsService(fr.lmdp.security.AdminUserRepository adminUserRepository) {
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        fr.lmdp.security.AdminUser user = adminUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu : " + username));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .roles("ADMIN")
                .build();
    }
}

