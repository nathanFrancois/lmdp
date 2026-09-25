package fr.lmdp.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Les lignes sont systématiquement affichées avec la commande : on les charge en une requête
    // (la vue n'a pas de session ouverte, open-in-view étant désactivé).
    @EntityGraph(attributePaths = "lines")
    Optional<Order> findByReference(String reference);

    @EntityGraph(attributePaths = "lines")
    Optional<Order> findByHostedCheckoutId(String hostedCheckoutId);

    @EntityGraph(attributePaths = "lines")
    List<Order> findAllByOrderByCreatedAtDesc();
}


