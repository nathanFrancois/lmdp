package fr.lmdp.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Accès aux produits en base de données. Le schéma est géré exclusivement
 * par Flyway (voir src/main/resources/db/migration).
 */
public interface ProductRepository extends JpaRepository<fr.lmdp.catalog.Product, String> {

    List<fr.lmdp.catalog.Product> findByAvailableTrue();
}

