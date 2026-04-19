package com.translationapp.payload.response;

import java.util.List;

public class DeepLResponse {
    private List<Translation> translations;

    public DeepLResponse() {}

    public List<Translation> getTranslations() { return translations; }
    public void setTranslations(List<Translation> translations) { this.translations = translations; }

    public static class Translation {
        private String text;
        private String detected_source_language;

        public Translation() {}

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getDetected_source_language() { return detected_source_language; }
        public void setDetected_source_language(String detected_source_language) { this.detected_source_language = detected_source_language; }
    }
}
