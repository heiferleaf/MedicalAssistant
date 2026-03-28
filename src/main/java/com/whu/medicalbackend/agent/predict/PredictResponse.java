package com.whu.medicalbackend.agent.predict;

import java.util.List;

public class PredictResponse {
    private String status;
    private List<Prediction> predictions;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Prediction> getPredictions() { return predictions; }
    public void setPredictions(List<Prediction> predictions) { this.predictions = predictions; }

    public static class Prediction {
        private Double probability;
        private String reaction;

        public Double getProbability() { return probability; }
        public void setProbability(Double probability) { this.probability = probability; }
        public String getReaction() { return reaction; }
        public void setReaction(String reaction) { this.reaction = reaction; }
    }
}