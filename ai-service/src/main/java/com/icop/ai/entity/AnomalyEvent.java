package com.icop.ai.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted record of a confirmed anomaly — the history behind the
 * /api/insights/anomalies endpoints. Indexed on service and detectedAt
 * because those are exactly the two ways the API queries this table.
 */
@Entity
@Table(name = "anomaly_events", indexes = {
        @Index(name = "idx_anomaly_service", columnList = "service"),
        @Index(name = "idx_anomaly_detected_at", columnList = "detectedAt")
})
public class AnomalyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String metric;

    private double currentValue;
    private double zScore;

    @Column(nullable = false)
    private Instant detectedAt;

    @PrePersist
    void prePersist() {
        if (detectedAt == null) detectedAt = Instant.now();
    }

    // JPA wants the no-arg constructor; everyone else uses the real one
    public AnomalyEvent() {}

    public AnomalyEvent(String service, String metric, double currentValue, double zScore) {
        this.service = service;
        this.metric = metric;
        this.currentValue = currentValue;
        this.zScore = zScore;
    }

    // read-only after creation — anomaly history shouldn't be editable
    public UUID getId() { return id; }
    public String getService() { return service; }
    public String getMetric() { return metric; }
    public double getCurrentValue() { return currentValue; }
    public double getzScore() { return zScore; }
    public Instant getDetectedAt() { return detectedAt; }
}
