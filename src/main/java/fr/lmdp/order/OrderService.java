package fr.lmdp.order;

import fr.lmdp.catalog.Product;
import fr.lmdp.catalog.ProductService;
import fr.lmdp.config.ShopProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Prise de commande et suivi de son cycle de vie. Tous les montants sont recalculés
 * ici à partir du catalogue : le panier du navigateur ne fournit que des identifiants
 * de produit et des quantités.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final OrderReferenceGenerator referenceGenerator;
    private final ShopProperties shopProperties;

    public OrderService(OrderRepository orderRepository, ProductService productService,
                        OrderReferenceGenerator referenceGenerator, ShopProperties shopProperties) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.referenceGenerator = referenceGenerator;
        this.shopProperties = shopProperties;
    }

    /** Crée une commande en attente de paiement à partir du panier soumis. */
    @Transactional
    public Order place(CheckoutRequest request) {
        List<OrderLine> lines = buildLines(request.cart());
        Order order = Order.create(referenceGenerator.generate(), request.customer(), request.deliveryMethod(),
                request.shippingAddress(), lines, shopProperties.shippingFee());
        Order saved = orderRepository.save(order);
        log.info("Commande {} créée ({} ligne(s), total {} {})", saved.getReference(), lines.size(),
                saved.getTotalAmount(), saved.getCurrency());
        return saved;
    }

    private List<OrderLine> buildLines(Cart cart) {
        List<OrderLine> lines = new ArrayList<>();
        for (Cart.CartItem item : cart.items()) {
            Product product = productService.findById(item.productId())
                    .filter(Product::isAvailable)
                    .orElseThrow(() -> new CheckoutException(
                            "Une création de votre panier n'est plus disponible. Merci de le mettre à jour."));
            if (product.getStock() < item.quantity()) {
                throw new CheckoutException("Stock insuffisant pour « " + product.getName() + " ».");
            }
            lines.add(OrderLine.of(product, item.quantity()));
        }
        if (lines.isEmpty()) {
            throw new CheckoutException("Votre panier est vide.");
        }
        return lines;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findByReference(String reference) {
        return orderRepository.findByReference(reference);
    }

    @Transactional(readOnly = true)
    public Order requireByReference(String reference) {
        return findByReference(reference)
                .orElseThrow(() -> new CheckoutException("Commande introuvable."));
    }

    @Transactional(readOnly = true)
    public List<Order> findAllRecentFirst() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Mémorise la session de paiement ouverte chez le prestataire pour cette commande. */
    @Transactional
    public void attachCheckout(Order order, String hostedCheckoutId) {
        order.attachCheckout(hostedCheckoutId);
        orderRepository.save(order);
    }

    /**
     * Confirme le paiement d'une commande. Le décompte du stock n'a lieu qu'au premier
     * appel : les notifications rejouées par CAWL sont ainsi sans effet supplémentaire.
     */
    @Transactional
    public void confirmPayment(Order order, String paymentId, String providerStatus) {
        boolean firstConfirmation = !order.isPaid();
        order.markPaid(paymentId, providerStatus);
        if (firstConfirmation) {
            order.getLines().forEach(line -> productService.decreaseStock(line.getProductId(), line.getQuantity()));
            log.info("Commande {} payée (paiement {})", order.getReference(), paymentId);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void rejectPayment(Order order, String providerStatus) {
        order.markPaymentFailed(providerStatus);
        orderRepository.save(order);
        log.info("Commande {} : paiement non abouti ({})", order.getReference(), providerStatus);
    }

    @Transactional
    public void recordPaymentProgress(Order order, String providerStatus) {
        order.recordPaymentStatus(providerStatus);
        orderRepository.save(order);
    }

    /** Transition manuelle demandée depuis le back-office. */
    @Transactional
    public void changeStatus(String reference, OrderStatus target) {
        Order order = requireByReference(reference);
        order.changeStatus(target);
        orderRepository.save(order);
        log.info("Commande {} passée au statut {}", reference, target);
    }
}


