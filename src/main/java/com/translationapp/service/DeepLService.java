package com.translationapp.service;

import com.translationapp.payload.response.DeepLResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeepLService {

    @Value("${deepl.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private static final String DEEPL_API_URL = "https://api-free.deepl.com/v2/translate";

    public DeepLService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String translate(String text, String sourceLang, String targetLang) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "DeepL-Auth-Key " + apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("text", new String[]{text});
            body.put("target_lang", targetLang.toUpperCase());
            if (sourceLang != null && !sourceLang.isEmpty()) {
                body.put("source_lang", sourceLang.toUpperCase());
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            DeepLResponse response = restTemplate.postForObject(DEEPL_API_URL, entity, DeepLResponse.class);

            if (response != null && response.getTranslations() != null && !response.getTranslations().isEmpty()) {
                return response.getTranslations().get(0).getText();
            }
        } catch (Exception e) {
            // In Produktion: Logging implementieren
            System.err.println("Fehler bei DeepL API-Aufruf: " + e.getMessage());
            return "Fehler bei der automatischen Übersetzung.";
        }
        return "";
    }
}