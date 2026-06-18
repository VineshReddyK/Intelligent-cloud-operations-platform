package com.icop.ai.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

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

    public AnomalyEvent() {}

    public AnomalyEvent(String service, String metric, double currentValue, double zScore) {
        this.service = service;
        this.metric = metric;
        this.currentValue = currentValue;
        this.zScore = zScore;
    }

    public UUID getId() { return id; }
    public String getService() { return service; }
    public String getMetric() { return metric; }
    public double getCurrentValue() { return currentValue; }
    public double getzScore() { return zScore; }
    public Instant getDetectedAt() { return detectedAt; }
}
