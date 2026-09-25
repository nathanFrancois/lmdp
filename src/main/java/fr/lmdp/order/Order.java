package fr.lmdp.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Commande passée depuis le panier. Les montants sont calculés côté serveur à partir
 * du catalogue, et le statut de paiement ne peut évoluer que sur confirmation vérifiée
 * du prestataire (webhook signé ou interrogation directe de l'API CAWL).
 */
@Entity
@Table(name = "orders")
public class Order {

    public static final String CURRENCY = "EUR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant public, non devinable : il circule dans les URLs et chez le prestataire. */
    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.AWAITING_PAYMENT;

    @Embedded
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 20)
    private DeliveryMethod deliveryMethod;

    @Embedded
    private ShippingAddress shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    @Column(name = "items_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemsTotal = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = CURRENCY;

    @Column(name = "hosted_checkout_id", length = 100)
    private String hostedCheckoutId;

    @Column(name = "payment_id", length = 100)
    private String paymentId;

    /** Dernier statut renvoyé par CAWL, conservé tel quel pour le support. */
    @Column(name = "payment_status", length = 40)
    private String paymentStatus;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Order() {
        // Requis par JPA/Hibernate.
    }

    public static Order create(String reference, Customer customer, DeliveryMethod deliveryMethod,
                               ShippingAddress shippingAddress, List<OrderLine> lines, BigDecimal shippingFee) {
        Order order = new Order();
        order.reference = reference;
        order.customer = customer;
        order.deliveryMethod = deliveryMethod;
        order.shippingAddress = deliveryMethod.requiresAddress() ? shippingAddress : null;
        lines.forEach(order::addLine);
        order.shippingFee = deliveryMethod.requiresAddress() ? shippingFee : BigDecimal.ZERO;
        order.recomputeTotals();
        return order;
    }

    private void addLine(OrderLine line) {
        line.attachTo(this);
        lines.add(line);
    }

    private void recomputeTotals() {
        itemsTotal = lines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalAmount = itemsTotal.add(shippingFee);
    }

    /** Mémorise la session de paiement ouverte chez CAWL. */
    public void attachCheckout(String hostedCheckoutId) {
        this.hostedCheckoutId = hostedCheckoutId;
        touch();
    }

    /**
     * Confirme le paiement. L'opération est idempotente : rejouer une notification
     * déjà traitée ne modifie pas la date de paiement ni la suite du traitement.
     */
    public void markPaid(String paymentId, String providerStatus) {
        this.paymentStatus = providerStatus;
        this.paymentId = paymentId;
        if (paidAt == null) {
            paidAt = Instant.now();
        }
        if (status == OrderStatus.AWAITING_PAYMENT || status == OrderStatus.PAYMENT_FAILED) {
            status = OrderStatus.PAID;
        }
        touch();
    }

    /** Paiement refusé ou abandonné : la commande reste consultable mais non honorée. */
    public void markPaymentFailed(String providerStatus) {
        recordPaymentStatus(providerStatus);
        if (status == OrderStatus.AWAITING_PAYMENT) {
            status = OrderStatus.PAYMENT_FAILED;
        }
        touch();
    }

    /** Statut intermédiaire (autorisation, capture demandée…) : informatif uniquement. */
    public void recordPaymentStatus(String providerStatus) {
        if (providerStatus != null) {
            this.paymentStatus = providerStatus;
        }
        touch();
    }

    /** Transition manuelle depuis le back-office, contrôlée par {@link OrderStatus}. */
    public void changeStatus(OrderStatus target) {
        if (!status.canMoveTo(target, deliveryMethod)) {
            throw new IllegalStateException(
                    "Transition interdite de " + status + " vers " + target + " pour la commande " + reference);
        }
        status = target;
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public boolean isPaid() {
        return paidAt != null;
    }

    public boolean isAwaitingPayment() {
        return status == OrderStatus.AWAITING_PAYMENT;
    }

    public long getTotalInMinorUnits() {
        return Money.toMinorUnits(totalAmount);
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getItemsTotal() {
        return itemsTotal;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getHostedCheckoutId() {
        return hostedCheckoutId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderStatus> getNextStatuses() {
        return status.nextStatuses(deliveryMethod);
    }
}

