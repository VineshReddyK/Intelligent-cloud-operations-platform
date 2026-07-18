package com.icop.ai.service;

import com.icop.ai.dto.MetricSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pulls the four signals the models care about — error rate, p99 latency,
 * request rate, kafka lag — straight out of Prometheus, one snapshot per
 * service per cycle. Prometheus already scrapes everyone, so this is much
 * cheaper than instrumenting every service to push metrics at us.
 */
@Service
public class MetricsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(MetricsCollectorService.class);

    // the fleet we watch. hardcoded is fine at this scale — service discovery
    // would be the upgrade if the platform grew past a handful of services
    private static final Map<String, Integer> SERVICES = Map.of(
            "api-gateway", 8080,
            "user-service", 8081,
            "order-service", 8082,
            "payment-service", 8083,
            "notification-service", 8084
    );

    private final RestTemplate restTemplate;
    private final String prometheusUrl;

    public MetricsCollectorService(RestTemplate restTemplate,
                                   @Value("${prometheus.url}") String prometheusUrl) {
        this.restTemplate = restTemplate;
        this.prometheusUrl = prometheusUrl;
    }

    public List<MetricSnapshot> collectAll() {
        // one unreachable service shouldn't sink the whole cycle — collect
        // what we can and move on
        List<MetricSnapshot> snapshots = new ArrayList<>();
        for (String service : SERVICES.keySet()) {
            collect(service).ifPresent(snapshots::add);
        }
        return snapshots;
    }

    private Optional<MetricSnapshot> collect(String service) {
        try {
            // the `or vector(0)` fallbacks matter: a service with zero traffic
            // returns an empty result set, and we want 0.0 there, not an error
            double errorRate = queryScalar(
                    "100 * sum(rate(http_server_requests_seconds_count{service=\"" + service + "\",outcome=\"SERVER_ERROR\"}[5m]))" +
                    " / (sum(rate(http_server_requests_seconds_count{service=\"" + service + "\"}[5m])) > 0) or vector(0)");

            double p99Ms = queryScalar(
                    "histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{service=\"" + service + "\"}[5m])) by (le)) * 1000");

            double requestRate = queryScalar(
                    "sum(rate(http_server_requests_seconds_count{service=\"" + service + "\"}[5m])) or vector(0)");

            double kafkaLag = queryScalar(
                    "sum(kafka_consumer_fetch_manager_records_lag{service=\"" + service + "\"}) or vector(0)");

            String cbState = queryCbState(service);

            return Optional.of(new MetricSnapshot(service, requestRate, errorRate, p99Ms, kafkaLag, cbState, Instant.now()));

        } catch (Exception e) {
            log.warn("Could not collect metrics for {}: {}", service, e.getMessage());
            return Optional.empty();
        }
    }

    // walk prometheus's response envelope down to the single number inside.
    // every missing layer means "no data", which for us is just 0.0
    @SuppressWarnings("unchecked")
    private double queryScalar(String promql) {
        String url = prometheusUrl + "/api/v1/query?query=" + URLEncoder.encode(promql, StandardCharsets.UTF_8);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) return 0.0;

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) return 0.0;

        List<Object> result = (List<Object>) data.get("result");
        if (result == null || result.isEmpty()) return 0.0;

        Map<String, Object> first = (Map<String, Object>) result.get(0);
        List<Object> value = (List<Object>) first.get("value");
        if (value == null || value.size() < 2) return 0.0;

        try {
            return Double.parseDouble(value.get(1).toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // resilience4j exports one series per breaker state with value 1 on the
    // active one — find that series and read its label
    @SuppressWarnings("unchecked")
    private String queryCbState(String service) {
        try {
            String promql = "resilience4j_circuitbreaker_state{service=\"" + service + "\"}";
            String url = prometheusUrl + "/api/v1/query?query=" + URLEncoder.encode(promql, StandardCharsets.UTF_8);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return "UNKNOWN";

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) return "UNKNOWN";

            List<Object> result = (List<Object>) data.get("result");
            if (result == null || result.isEmpty()) return "NONE"; // service has no breaker

            for (Object item : result) {
                Map<String, Object> entry = (Map<String, Object>) item;
                Map<String, Object> metric = (Map<String, Object>) entry.get("metric");
                List<Object> value = (List<Object>) entry.get("value");
                if (metric != null && value != null && "1".equals(value.get(1).toString())) {
                    return metric.getOrDefault("state", "UNKNOWN").toString().toUpperCase();
                }
            }
            return "CLOSED";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
