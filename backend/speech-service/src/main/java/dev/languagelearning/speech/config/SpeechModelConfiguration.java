package dev.languagelearning.speech.config;

import dev.languagelearning.config.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

/**
 * Configuration for speech models (text-to-speech and speech-to-text).
 * <p>
 * Creates OpenAI-compatible audio models that can work with:
 * <ul>
 *   <li>OpenAI's official API (default)</li>
 *   <li>Local Piper TTS server (OpenAI-compatible)</li>
 *   <li>Local Whisper STT server (OpenAI-compatible)</li>
 * </ul>
 * <p>
 * This configuration supports different servers for TTS and STT by creating
 * separate API clients for each service.
 * <p>
 * To enable with local services:
 * <pre>
 * OPENAI_SPEECH_ENABLED=true
 * OPENAI_TRANSCRIPTION_ENABLED=true
 * TTS_BASE_URL=http://localhost:9001
 * STT_BASE_URL=http://localhost:9000
 * TTS_API_KEY=local-key
 * STT_API_KEY=local-key
 * </pre>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.openai.audio.speech.enabled", havingValue = "true")
public class SpeechModelConfiguration {

    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";
    private static final String DUMMY_API_KEY = "local-service-no-key-required";

    /**
     * Creates the OpenAI Audio API client for text-to-speech.
     *
     * @param aiConfig the AI provider configuration
     * @return the OpenAI Audio API client for TTS
     */
    @Bean
    @Qualifier("ttsAudioApi")
    public OpenAiAudioApi openAiAudioApiForTts(AiProviderConfig aiConfig) {
        var ttsConfig = aiConfig.getTextToSpeech();

        String apiKey = resolveApiKey(ttsConfig, aiConfig);
        String baseUrl = resolveBaseUrl(ttsConfig);

        log.info("Configuring OpenAI Audio API (TTS) with base URL: {}", baseUrl);

        RestClient.Builder restClientBuilder = createRestClientBuilder();
        return new OpenAiAudioApi(baseUrl, apiKey, restClientBuilder, null);
    }

    /**
     * Creates the OpenAI Audio Speech Model for text-to-speech.
     *
     * @param ttsAudioApi the OpenAI Audio API client for TTS
     * @return the speech model
     */
    @Bean
    public OpenAiAudioSpeechModel openAiAudioSpeechModel(@Qualifier("ttsAudioApi") OpenAiAudioApi ttsAudioApi) {
        log.info("Creating OpenAI Audio Speech Model for text-to-speech");
        return new OpenAiAudioSpeechModel(ttsAudioApi);
    }

    /**
     * Creates the OpenAI Audio API client for speech-to-text.
     * <p>
     * This is separate from the TTS API client to support different
     * servers for TTS (e.g., Piper on port 9001) and STT (e.g., Whisper on port 9000).
     *
     * @param aiConfig the AI provider configuration
     * @return the OpenAI Audio API client for STT
     */
    @Bean
    @Qualifier("sttAudioApi")
    @ConditionalOnProperty(name = "spring.ai.openai.audio.transcription.enabled", havingValue = "true")
    public OpenAiAudioApi openAiAudioApiForStt(AiProviderConfig aiConfig) {
        var sttConfig = aiConfig.getSpeechToText();

        String apiKey = resolveApiKeyForStt(sttConfig, aiConfig);
        String baseUrl = resolveBaseUrlForStt(sttConfig);

        log.info("Configuring OpenAI Audio API (STT) with base URL: {}", baseUrl);

        RestClient.Builder restClientBuilder = createRestClientBuilder();
        return new OpenAiAudioApi(baseUrl, apiKey, restClientBuilder, null);
    }

    /**
     * Creates the OpenAI Audio Transcription Model for speech-to-text.
     *
     * @param sttAudioApi the OpenAI Audio API client for STT
     * @return the transcription model
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.audio.transcription.enabled", havingValue = "true")
    public OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel(@Qualifier("sttAudioApi") OpenAiAudioApi sttAudioApi) {
        log.info("Creating OpenAI Audio Transcription Model for speech-to-text");
        return new OpenAiAudioTranscriptionModel(sttAudioApi);
    }

    private RestClient.Builder createRestClientBuilder() {
        return RestClient.builder()
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new RuntimeException("OpenAI Audio API error: " + response.getStatusCode());
                });
    }

    private String resolveApiKey(AiProviderConfig.TextToSpeechConfig ttsConfig, AiProviderConfig aiConfig) {
        if (ttsConfig != null && ttsConfig.getApiKey().isPresent()) {
            return ttsConfig.getApiKey().get();
        }
        // Fall back to LLM API key
        return aiConfig.getLlm().getApiKey().orElse(DUMMY_API_KEY);
    }

    private String resolveApiKeyForStt(AiProviderConfig.SpeechToTextConfig sttConfig, AiProviderConfig aiConfig) {
        if (sttConfig != null && sttConfig.getApiKey().isPresent()) {
            return sttConfig.getApiKey().get();
        }
        // Fall back to LLM API key
        return aiConfig.getLlm().getApiKey().orElse(DUMMY_API_KEY);
    }

    private String resolveBaseUrl(AiProviderConfig.TextToSpeechConfig ttsConfig) {
        if (ttsConfig != null && ttsConfig.getBaseUrl().isPresent()) {
            return ttsConfig.getBaseUrl().get();
        }
        return DEFAULT_OPENAI_BASE_URL;
    }

    private String resolveBaseUrlForStt(AiProviderConfig.SpeechToTextConfig sttConfig) {
        if (sttConfig != null && sttConfig.getBaseUrl().isPresent()) {
            return sttConfig.getBaseUrl().get();
        }
        return DEFAULT_OPENAI_BASE_URL;
    }
}