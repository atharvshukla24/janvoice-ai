package com.janvoice.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.janvoice.ai.dto.GeminiAnalysisResult;
import com.janvoice.ai.dto.GeminiDuplicateCheckResult;
import com.janvoice.ai.entity.Complaint;
import com.janvoice.ai.entity.Urgency;
import com.janvoice.ai.service.GeminiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation connecting Spring Boot to Google Gemini API
 * using Java's native HttpClient and Jackson's ObjectMapper for JSON
 * serialization.
 */
@Service
public class GeminiServiceImpl implements GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Translates local dialects, classifies category and calculates priority
     * status.
     */
    @Override
    public GeminiAnalysisResult analyzeComplaint(String rawText) {
        String prompt = "You are a professional civic grievance analyzer. " +
                "Analyze the following citizen complaint and return a strictly formatted JSON response. " +
                "Rules:\n" +
                "1. If the input language is not English, detect it and translate 'translatedText' to English.\n" +
                "2. Classify 'category' strictly, one of: Roads, Water, Electricity, Healthcare, Education, Sanitation, Crime, Public Transport, Environment.\n"
                +
                "3. Set 'urgency' matching one of: LOW, MEDIUM, HIGH, CRITICAL. Factor safety and hazards.\n" +
                "4. Estimate your classification 'confidence' (0.0 to 1.0).\n\n" +
                "JSON Schema output:\n" +
                "{\n" +
                "  \"language\": \"detect language ISO-639-1 code\",\n" +
                "  \"translatedText\": \"translated description in English\",\n" +
                "  \"category\": \"Strict category name\",\n" +
                "  \"urgency\": \"Strict urgency level\",\n" +
                "  \"confidence\": 0.95\n" +
                "}\n\n" +
                "Complaint Text: \"" + rawText.replace("\"", "\\\"") + "\"";

        try {
            String rawJson = queryGemini(prompt);
            return objectMapper.readValue(rawJson, GeminiAnalysisResult.class);
        } catch (Exception e) {
            System.err.println("Gemini analysis error: " + e.getMessage());
            // Fallback for offline or invalid API keys key during hackathon demos
            return GeminiAnalysisResult.builder()
                    .language("en")
                    .translatedText(rawText)
                    .category("Roads")
                    .urgency("MEDIUM")
                    .confidence(0.70)
                    .build();
        }
    }

    /**
     * Deduplicates incoming grievances by cross-referencing previous reports.
     */
    @Override
    public GeminiDuplicateCheckResult checkForDuplicates(Complaint target, List<Complaint> existingComplaints) {
        if (existingComplaints == null || existingComplaints.isEmpty()) {
            return GeminiDuplicateCheckResult.builder().isDuplicate(false).build();
        }

        // Format active complains list to send to Gemini
        String activeList = existingComplaints.stream()
                .map(c -> String.format("- ID: %d | Content: %s", c.getId(), c.getTranslatedText()))
                .collect(Collectors.joining("\n"));

        String prompt = "You are a duplicate complaint detector. Compare target complaint against active issue logs.\n"
                +
                "Determine if target refers to the SAME underlying civic issue in the area (e.g. same pothole, same garbage pile, same leakage).\n\n"
                +
                "Active Complaints:\n" + activeList + "\n\n" +
                "Target Complaint: \"" + target.getTranslatedText() + "\"\n\n" +
                "Return a strictly formatted JSON response:\n" +
                "{\n" +
                "  \"isDuplicate\": true/false,\n" +
                "  \"matchedComplaintId\": matched_integer_or_null,\n" +
                "  \"explanation\": \"Brief rationale detailing structural match patterns\"\n" +
                "}";

        try {
            String rawJson = queryGemini(prompt);
            return objectMapper.readValue(rawJson, GeminiDuplicateCheckResult.class);
        } catch (Exception e) {
            System.err.println("Gemini duplicate check error: " + e.getMessage());
            return GeminiDuplicateCheckResult.builder().isDuplicate(false).build();
        }
    }


    /**
     * Synthesizes structural complaints list into a high-level briefing.
     */
    @Override
    public String generateAreaBriefing(String wardArea, List<Complaint> activeComplaints) {
        if (activeComplaints == null || activeComplaints.isEmpty()) {
            return "No active grievances logged in Ward Area: " + wardArea;
        }

        String complaintLog = activeComplaints.stream()
                .map(c -> String.format("- [%s] Status: %s | Issue: %s [Upvotes: %d]",
                        c.getCategory(), c.getStatus(), c.getTranslatedText(), c.getUpvotes()))
                .collect(Collectors.joining("\n"));

        String prompt = "Provide a concise executive briefing (max 150 words) for the Member of Parliament (MP) " +
                "summarizing active civic grievances in the constituency ward: " + wardArea + ".\n\n" +
                "Active Grievance Log:\n" + complaintLog + "\n\n" +
                "Compile key urgency factors and dominant categories in a short digest. Do not repeat greeting text. Output only plain informative text.";

        try {
            // A simple plain text return response from Gemini is fine for summary
            return queryGeminiRawText(prompt);
        } catch (Exception e) {
            System.err.println("Gemini briefing error: " + e.getMessage());
            return "Briefing generation unavailable. Active complaint count: " + activeComplaints.size();
        }
    }

    /**
     * Execute Gemini API REST request. Formats request structure and returns the
     * inner text.
     */
    private String queryGeminiRawText(String prompt) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Gemini API Key is missing. Configure GEMINI_API_KEY environment variable.");
        }

        // Construct Gemini request schema structure
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> partsObj = new HashMap<>();
        partsObj.put("parts", List.of(textPart));

        Map<String, Object> contentObj = new HashMap<>();
        contentObj.put("contents", List.of(partsObj));

        String requestBodyJson = objectMapper.writeValueAsString(contentObj);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Gemini HTTP Error. Status: " + response.statusCode() + " | Body: " + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText().trim();
    }

    /**
     * Execute Gemini API REST request, enforcing a JSON output configuration
     * schema.
     */
    private String queryGemini(String prompt) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Gemini API Key is missing. Configure GEMINI_API_KEY environment variable.");
        }

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> partsObj = new HashMap<>();
        partsObj.put("parts", List.of(textPart));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("contents", List.of(partsObj));
        requestBodyMap.put("generationConfig", generationConfig);

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Gemini HTTP Error. Status: " + response.statusCode() + " | Body: " + response.body());
        }

        JsonNode rootNode = objectMapper.readTree(response.body());
        return rootNode.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText().trim();
    }
}

