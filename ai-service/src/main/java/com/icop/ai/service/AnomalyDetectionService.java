package com.icop.ai.service;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import com.icop.ai.dto.AnomalyResult;
import com.icop.ai.dto.MetricSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects metric anomalies using a sliding window Z-score model backed by
 * DJL NDArray tensor operations (PyTorch engine).
 *
 * Z-score = |x - μ| / σ  where μ and σ are computed over the last N samples.
 * A score above the configured threshold signals an anomaly.
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);
    private static final int MIN_WINDOW_FOR_DETECTION = 10;

    private final NDManager ndManager;
    private final int windowSize;
    private final double zScoreThreshold;

    // key: "service:metric" → ordered ring buffer of historical values
    private final Map<String, Deque<Double>> metricWindows = new ConcurrentHashMap<>();

    public AnomalyDetectionService(
            NDManager ndManager,
            @Value("${ai.anomaly.window-size:60}") int windowSize,
            @Value("${ai.anomaly.z-score-threshold:2.5}") double zScoreThreshold) {
        this.ndManager = ndManager;
        this.windowSize = windowSize;
        this.zScoreThreshold = zScoreThreshold;
    }

    public List<AnomalyResult> detect(MetricSnapshot snapshot) {
        List<AnomalyResult> results = new ArrayList<>();

        results.add(score(snapshot.service(), "error_rate_pct", snapshot.errorRatePercent()));
        results.add(score(snapshot.service(), "p99_latency_ms", snapshot.p99LatencyMs()));
        results.add(score(snapshot.service(), "request_rate_rps", snapshot.requestRatePerSecond()));
        results.add(score(snapshot.service(), "kafka_lag", snapshot.kafkaLag()));

        results.stream()
                .filter(AnomalyResult::anomaly)
                .forEach(r -> log.warn("ANOMALY detected: service={} metric={} value={:.3f} z-score={:.3f}",
                        r.service(), r.metric(), r.currentValue(), r.zScore()));

        return results;
    }

    private AnomalyResult score(String service, String metric, double currentValue) {
        String key = service + ":" + metric;
        Deque<Double> window = metricWindows.computeIfAbsent(key, k -> new ArrayDeque<>(windowSize));

        boolean isAnomaly = false;
        double zScore = 0.0;

        if (window.size() >= MIN_WINDOW_FOR_DETECTION) {
            float[] values = toFloatArray(window);

            // DJL PyTorch NDArray tensor ops: compute mean and std over the sliding window
            try (NDArray data = ndManager.create(values)) {
                float mean = data.mean().getFloat();
                // population variance: E[(x - μ)²]
                float variance = data.sub(mean).pow(2).mean().getFloat();
                float std = (float) Math.sqrt(variance + 1e-8f); // epsilon prevents div-by-zero
                zScore = Math.abs((currentValue - mean) / std);
                isAnomaly = zScore > zScoreThreshold;
            } catch (Exception e) {
                log.debug("NDArray computation skipped for {}: {}", key, e.getMessage());
            }
        }

        // Slide the window
        if (window.size() >= windowSize) window.pollFirst();
        window.addLast(currentValue);

        return new AnomalyResult(service, metric, currentValue, zScore, isAnomaly);
    }

    private float[] toFloatArray(Deque<Double> deque) {
        float[] arr = new float[deque.size()];
        int i = 0;
        for (double v : deque) arr[i++] = (float) v;
        return arr;
    }

    public Map<String, Integer> windowStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        metricWindows.forEach((k, v) -> stats.put(k, v.size()));
        return stats;
    }
}
