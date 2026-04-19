package com.translationapp.controller;

import com.translationapp.service.DeepLService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TranslationControllerTest {

    @Test
    void testAutoTranslate_Success() {
        // Given
        DeepLService fakeDeepLService = new DeepLService(null) {
            @Override
            public String translate(String text, String sourceLang, String targetLang) {
                if ("Hello".equals(text) && "EN".equals(sourceLang) && "DE".equals(targetLang)) {
                    return "Hallo";
                }
                return "Error";
            }
        };

        TranslationController controller = new TranslationController(fakeDeepLService);
        Map<String, String> request = Map.of(
                "text", "Hello",
                "sourceLang", "EN",
                "targetLang", "DE"
        );

        // When
        ResponseEntity<?> response = controller.autoTranslate(request);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body);
        assertEquals("Hallo", body.get("translatedText"));
    }
}
