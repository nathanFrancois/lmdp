package fr.lmdp.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Accès aux produits en base de données. Le schéma est géré exclusivement
 * par Flyway (voir src/main/resources/db/migration).
 */
public interface ProductRepository extends JpaRepository<fr.lmdp.catalog.Product, String> {

    List<fr.lmdp.catalog.Product> findByAvailableTrue();

    @org.springframework.data.jpa.repository.Query(value =
            "SELECT id FROM catalog_feature_lock WHERE id = 1 FOR UPDATE", nativeQuery = true)
    Integer lockFeaturedSelection();

    List<Product> findByFeaturedTrue();

    /** Produits vendus tels quels, affichés sur la page "Créations". */
    List<fr.lmdp.catalog.Product> findByAvailableTrueAndCustomizableFalse();

    /** Produits à personnaliser sur devis, affichés sur la page "Personnalisation". */
    List<fr.lmdp.catalog.Product> findByAvailableTrueAndCustomizableTrue();
}

