package dev.languagelearning.speech.config;

import dev.languagelearning.config.AiProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

/**
 * Configuration for speech models (text-to-speech and speech-to-text).
 * <p>
 * Creates OpenAI audio models when both conditions are met:
 * <ul>
 *   <li>The TTS provider is set to "openai" ({@code TTS_PROVIDER=openai})</li>
 *   <li>An API key is provided ({@code TTS_API_KEY} or {@code LLM_API_KEY})</li>
 * </ul>
 * <p>
 * This configuration is disabled by default. To enable speech services with OpenAI:
 * <pre>
 * TTS_PROVIDER=openai
 * TTS_API_KEY=sk-your-openai-api-key
 * </pre>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.ai.openai.audio.speech.enabled", havingValue = "true")
public class SpeechModelConfiguration {

    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";

    /**
     * Creates the OpenAI Audio API client.
     *
     * @param aiConfig the AI provider configuration
     * @return the OpenAI Audio API client
     */
    @Bean
    public OpenAiAudioApi openAiAudioApi(AiProviderConfig aiConfig) {
        var ttsConfig = aiConfig.getTextToSpeech();

        String apiKey = resolveApiKey(ttsConfig, aiConfig);
        String baseUrl = resolveBaseUrl(ttsConfig);

        log.info("Configuring OpenAI Audio API with base URL: {}", baseUrl);

        RestClient.Builder restClientBuilder = RestClient.builder()
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    throw new RuntimeException("OpenAI Audio API error: " + response.getStatusCode());
                });

        return new OpenAiAudioApi(baseUrl, apiKey, restClientBuilder, null);
    }

    /**
     * Creates the OpenAI Audio Speech Model for text-to-speech.
     *
     * @param audioApi the OpenAI Audio API client
     * @return the speech model
     */
    @Bean
    public OpenAiAudioSpeechModel openAiAudioSpeechModel(OpenAiAudioApi audioApi) {
        log.info("Creating OpenAI Audio Speech Model for text-to-speech");
        return new OpenAiAudioSpeechModel(audioApi);
    }

    /**
     * Creates the OpenAI Audio Transcription Model for speech-to-text.
     *
     * @param audioApi the OpenAI Audio API client
     * @return the transcription model
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.audio.transcription.enabled", havingValue = "true")
    public OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel(OpenAiAudioApi audioApi) {
        log.info("Creating OpenAI Audio Transcription Model for speech-to-text");
        return new OpenAiAudioTranscriptionModel(audioApi);
    }

    private String resolveApiKey(AiProviderConfig.TextToSpeechConfig ttsConfig, AiProviderConfig aiConfig) {
        if (ttsConfig != null) {
            return ttsConfig.getApiKey()
                    .orElseGet(() -> aiConfig.getLlm().getApiKey().orElse(""));
        }
        return aiConfig.getLlm().getApiKey().orElse("");
    }

    private String resolveBaseUrl(AiProviderConfig.TextToSpeechConfig ttsConfig) {
        if (ttsConfig != null) {
            return ttsConfig.getBaseUrl().orElse(DEFAULT_OPENAI_BASE_URL);
        }
        return DEFAULT_OPENAI_BASE_URL;
    }
}
