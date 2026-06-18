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
