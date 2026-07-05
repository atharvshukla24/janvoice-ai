package com.janvoice.ai.dto;

/**
 * Data Transfer Object representing the structured analysis output
 * from Google Gemini for a single raw citizen complaint.
 * Standard Java implementation (No Lombok annotation wrappers).
 */
public class GeminiAnalysisResult {
    private String language;
    private String translatedText;
    private String category;
    private String urgency;
    private Double confidence;

    // Default Constructor
    public GeminiAnalysisResult() {
    }

    // Full Constructor
    public GeminiAnalysisResult(String language, String translatedText, String category, String urgency,
            Double confidence) {
        this.language = language;
        this.translatedText = translatedText;
        this.category = category;
        this.urgency = urgency;
        this.confidence = confidence;
    }

    // Getters and Setters
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    // Builder
    public static GeminiAnalysisResultBuilder builder() {
        return new GeminiAnalysisResultBuilder();
    }

    public static class GeminiAnalysisResultBuilder {
        private String language;
        private String translatedText;
        private String category;
        private String urgency;
        private Double confidence;

        public GeminiAnalysisResultBuilder language(String language) {
            this.language = language;
            return this;
        }

        public GeminiAnalysisResultBuilder translatedText(String translatedText) {
            this.translatedText = translatedText;
            return this;
        }

        public GeminiAnalysisResultBuilder category(String category) {
            this.category = category;
            return this;
        }

        public GeminiAnalysisResultBuilder urgency(String urgency) {
            this.urgency = urgency;
            return this;
        }

        public GeminiAnalysisResultBuilder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public GeminiAnalysisResult build() {
            return new GeminiAnalysisResult(language, translatedText, category, urgency, confidence);
        }
    }
}
