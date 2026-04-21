package com.translationapp.service;

import com.translationapp.payload.response.DeepLResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.match.MockRestRequestMatchers; // Explicit import for clarity

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"deepl.api.key=valid-test-key"})
class DeepLServiceTest {

    @Autowired
    private DeepLService deepLService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private static final String TEST_API_KEY = "valid-test-key"; // Placeholder API key for tests
    private static final String DEEPL_API_URL = "https://api.deepl.com/v2/translate";

    @BeforeEach
    void setUp() {
        this.mockServer = MockRestServiceServer.bindTo(this.restTemplate).build();
    }

    @Test
    void testTranslate_Success() throws Exception {
        String textToTranslate = "Hello world";
        String sourceLang = "EN";
        String targetLang = "DE";
        String expectedTranslatedText = "Hallo Welt";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("text", new String[]{textToTranslate});
        requestBodyMap.put("target_lang", targetLang.toUpperCase());
        requestBodyMap.put("source_lang", sourceLang.toUpperCase());
        String requestBodyJson = convertMapToJson(requestBodyMap);

        String responseBodyJson = "{\"translations\":[{\"text\":\"" + expectedTranslatedText + "\",\"detected_source_language\":\"EN\"}]}";

        this.mockServer.expect(requestTo(DEEPL_API_URL))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.header("Authorization", "DeepL-Auth-Key " + TEST_API_KEY))
                .andExpect(MockRestRequestMatchers.content().json(requestBodyJson))
                .andRespond(withSuccess(responseBodyJson, MediaType.APPLICATION_JSON));

        String result = deepLService.translate(textToTranslate, sourceLang, targetLang);
        assertEquals(expectedTranslatedText, result);
        this.mockServer.verify();
    }

    @Test
    void testTranslate_ApiError() throws Exception {
        String textToTranslate = "Hello world";
        String sourceLang = "EN";
        String targetLang = "DE";
        String expectedErrorMessage = "[FEHLER-MOCK] Hello world";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("text", new String[]{textToTranslate});
        requestBodyMap.put("target_lang", targetLang.toUpperCase());
        requestBodyMap.put("source_lang", sourceLang.toUpperCase());
        String requestBodyJson = convertMapToJson(requestBodyMap);

        this.mockServer.expect(requestTo(DEEPL_API_URL))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.header("Authorization", "DeepL-Auth-Key " + TEST_API_KEY))
                .andExpect(MockRestRequestMatchers.content().json(requestBodyJson))
                .andRespond(withServerError());

        String result = deepLService.translate(textToTranslate, sourceLang, targetLang);
        assertEquals(expectedErrorMessage, result);
        this.mockServer.verify();
    }

    @Test
    void testTranslate_NoSourceLang() throws Exception {
        String textToTranslate = "Hello world";
        String sourceLang = ""; // Empty sourceLang
        String targetLang = "FR";
        String expectedTranslatedText = "Bonjour le monde";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("text", new String[]{textToTranslate});
        requestBodyMap.put("target_lang", targetLang.toUpperCase());
        String requestBodyJson = convertMapToJson(requestBodyMap);

        String responseBodyJson = "{\"translations\":[{\"text\":\"" + expectedTranslatedText + "\",\"detected_source_language\":\"EN\"}]}";

        this.mockServer.expect(requestTo(DEEPL_API_URL))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.header("Authorization", "DeepL-Auth-Key " + TEST_API_KEY))
                .andExpect(MockRestRequestMatchers.content().json(requestBodyJson))
                .andRespond(withSuccess(responseBodyJson, MediaType.APPLICATION_JSON));

        String result = deepLService.translate(textToTranslate, sourceLang, targetLang);
        assertEquals(expectedTranslatedText, result);
        this.mockServer.verify();
    }

    @Test
    void testTranslate_EmptyResponse() throws Exception {
        String textToTranslate = "Hello world";
        String sourceLang = "EN";
        String targetLang = "DE";
        String expectedResult = ""; // Service returns "" for empty/malformed responses

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("text", new String[]{textToTranslate});
        requestBodyMap.put("target_lang", targetLang.toUpperCase());
        requestBodyMap.put("source_lang", sourceLang.toUpperCase());
        String requestBodyJson = convertMapToJson(requestBodyMap);

        String responseBodyJson = "{}"; // Empty JSON

        this.mockServer.expect(requestTo(DEEPL_API_URL))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.header("Authorization", "DeepL-Auth-Key " + TEST_API_KEY))
                .andExpect(MockRestRequestMatchers.content().json(requestBodyJson))
                .andRespond(withSuccess(responseBodyJson, MediaType.APPLICATION_JSON));

        String result = deepLService.translate(textToTranslate, sourceLang, targetLang);
        assertEquals(expectedResult, result);
        this.mockServer.verify();
    }

    @Test
    void testTranslate_HttpError() throws Exception {
        String textToTranslate = "Hello world";
        String sourceLang = "EN";
        String targetLang = "DE";
        String expectedErrorMessage = "[FEHLER-MOCK] Hello world";

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("text", new String[]{textToTranslate});
        requestBodyMap.put("target_lang", targetLang.toUpperCase());
        requestBodyMap.put("source_lang", sourceLang.toUpperCase());
        String requestBodyJson = convertMapToJson(requestBodyMap);

        this.mockServer.expect(requestTo(DEEPL_API_URL))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andExpect(MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockRestRequestMatchers.header("Authorization", "DeepL-Auth-Key " + TEST_API_KEY))
                .andExpect(MockRestRequestMatchers.content().json(requestBodyJson))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        String result = deepLService.translate(textToTranslate, sourceLang, targetLang);
        assertEquals(expectedErrorMessage, result);
        this.mockServer.verify();
    }
    
    private String convertMapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String[]) {
                // Handle String array specifically for "text" field
                String[] arr = (String[]) entry.getValue();
                json.append("[");
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(arr[i]).append("\"");
                }
                json.append("]");
            } else if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else {
                // Fallback for other types, though not expected for this API call
                json.append(entry.getValue()); 
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}
