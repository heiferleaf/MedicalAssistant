package com.whu.medicalbackend.agent.predict;

import com.whu.medicalbackend.agent.flask.UnifiedFlaskClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PredictService {

    private final UnifiedFlaskClient unifiedFlaskClient;

    public PredictService(UnifiedFlaskClient unifiedFlaskClient) {
        this.unifiedFlaskClient = unifiedFlaskClient;
    }

    @SuppressWarnings("unchecked")
    public PredictResponse analyzeText(PredictRequest request) {
        Map<String, Object> result = unifiedFlaskClient.analyzePredict(request.getText());

        PredictResponse response = new PredictResponse();
        response.setStatus((String) result.get("status"));

        if (result.get("predictions") != null) {
            var predictions = ((java.util.List<Map<String, Object>>) result.get("predictions"))
                    .stream()
                    .map(m -> {
                        PredictResponse.Prediction p = new PredictResponse.Prediction();
                        p.setProbability((Double) m.get("probability"));
                        p.setReaction((String) m.get("reaction"));
                        return p;
                    })
                    .toList();
            response.setPredictions(predictions);
        }

        return response;
    }
}
