package com.translationapp.controller;

import com.translationapp.service.DeepLService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/translation")
public class TranslationController {

    private final DeepLService deepLService;

    public TranslationController(DeepLService deepLService) {
        this.deepLService = deepLService;
    }

    @PostMapping("/auto")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> autoTranslate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String sourceLang = request.get("sourceLang");
        String targetLang = request.get("targetLang");

        String translatedText = deepLService.translate(text, sourceLang, targetLang);

        return ResponseEntity.ok(Map.of("translatedText", translatedText));
    }
}