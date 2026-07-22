package com.icop.operator.service;

import com.icop.operator.dto.FailurePrediction;
import com.icop.operator.dto.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * The operator's one link to the AI service. Pulls the predictions list and
 * picks out the target service's risk.
 *
 * Crucially, any failure here returns UNKNOWN rather than throwing — the AI
 * service being down should never take the operator down with it. UNKNOWN
 * then resolves to a safe baseline in ScalingDecisionService.
 */
@Service
public class AiInsightClient {

    private static final Logger log = LoggerFactory.getLogger(AiInsightClient.class);

    private final RestTemplate restTemplate;

    public AiInsightClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RiskLevel getRiskLevel(String aiServiceUrl, String targetService) {
        try {
            String url = aiServiceUrl + "/api/insights/predictions";
            // ParameterizedTypeReference so Jackson keeps the generic —
            // otherwise the list deserializes to LinkedHashMaps, not records
            List<FailurePrediction> predictions = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<FailurePrediction>>() {}
            ).getBody();

            if (predictions == null || predictions.isEmpty()) return RiskLevel.UNKNOWN;

            return predictions.stream()
                    .filter(p -> targetService.equals(p.service()))
                    .map(FailurePrediction::riskLevel)
                    .findFirst()
                    .orElse(RiskLevel.UNKNOWN);

        } catch (Exception e) {
            log.warn("Failed to fetch AI risk level for service={} from {}: {}", targetService, aiServiceUrl, e.getMessage());
            return RiskLevel.UNKNOWN;
        }
    }
}
