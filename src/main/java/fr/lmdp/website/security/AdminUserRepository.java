package fr.lmdp.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<fr.lmdp.security.AdminUser, Long> {

    Optional<fr.lmdp.security.AdminUser> findByUsernameIgnoreCase(String username);
}

