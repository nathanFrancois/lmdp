package fr.lmdp.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Trace d'une notification CAWL déjà traitée. La clé primaire est l'identifiant
 * d'événement du prestataire : un rejeu est détecté et ignoré.
 */
@Entity
@Table(name = "payment_events")
public class PaymentEvent {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "event_type", length = 60)
    private String eventType;

    @Column(name = "order_reference", length = 30)
    private String orderReference;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected PaymentEvent() {
        // Requis par JPA/Hibernate.
    }

    public PaymentEvent(String eventId, String eventType, String orderReference) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.orderReference = orderReference;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}

