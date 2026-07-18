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
 * Sliding-window z-score anomaly detection, with the tensor math done on
 * DJL's PyTorch engine.
 *
 * z = |x - μ| / σ over the last N samples of each metric. Above the
 * threshold → anomaly. Simple, explainable, and it doesn't need training
 * data — which beats a fancier model you can't explain at 3am during an
 * incident.
 */
@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);
    // below this many samples the std is basically noise, so don't even score
    private static final int MIN_WINDOW_FOR_DETECTION = 10;

    private final NDManager ndManager;
    private final int windowSize;
    private final double zScoreThreshold;

    // "service:metric" → ring buffer of recent values. ConcurrentHashMap since
    // the scheduler and the REST refresh endpoint can both land here
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

        // each metric gets its own independent window — a latency spike and an
        // error spike are different stories even on the same service
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

            // mean + population variance on the GPU-capable path
            try (NDArray data = ndManager.create(values)) {
                float mean = data.mean().getFloat();
                float variance = data.sub(mean).pow(2).mean().getFloat();
                float std = (float) Math.sqrt(variance + 1e-8f); // epsilon: flat windows have σ=0
                zScore = Math.abs((currentValue - mean) / std);
                isAnomaly = zScore > zScoreThreshold;
            } catch (Exception e) {
                // same math in plain java — keeps detection alive on boxes where
                // the native PyTorch engine won't load (looking at you, CI)
                float[] vals = toFloatArray(window);
                double mean = 0.0;
                for (float v : vals) mean += v;
                mean /= vals.length;
                double variance = 0.0;
                for (float v : vals) variance += (v - mean) * (v - mean);
                variance /= vals.length;
                double std = Math.sqrt(variance + 1e-8);
                zScore = Math.abs((currentValue - mean) / std);
                isAnomaly = zScore > zScoreThreshold;
            }
        }

        // score first, THEN admit the value — otherwise a spike would drag the
        // mean toward itself and mask its own anomaly
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

    // exposed on /api/insights/health so you can see how warmed-up each window is
    public Map<String, Integer> windowStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        metricWindows.forEach((k, v) -> stats.put(k, v.size()));
        return stats;
    }
}
