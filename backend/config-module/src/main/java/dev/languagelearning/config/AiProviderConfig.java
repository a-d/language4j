package dev.languagelearning.config;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Optional;

/**
 * Configuration for AI provider settings.
 * <p>
 * Supports multiple AI providers for different capabilities:
 * <ul>
 *   <li>Text generation (LLM) - OpenAI, Anthropic, Ollama</li>
 *   <li>Speech-to-text (transcription) - OpenAI Whisper, etc.</li>
 *   <li>Text-to-speech (synthesis) - OpenAI TTS, ElevenLabs, etc.</li>
 *   <li>Image generation - DALL-E, Stable Diffusion, etc.</li>
 * </ul>
 * <p>
 * Example configuration:
 * <pre>{@code
 * language-learning:
 *   ai:
 *     llm:
 *       provider: openai
 *       api-key: ${OPENAI_API_KEY}
 *       model: gpt-4-turbo-preview
 *     speech-to-text:
 *       provider: openai
 *       api-key: ${OPENAI_API_KEY}
 *       model: whisper-1
 *     text-to-speech:
 *       provider: openai
 *       api-key: ${OPENAI_API_KEY}
 *       model: tts-1
 *       voice: alloy
 *     image:
 *       provider: openai
 *       api-key: ${OPENAI_API_KEY}
 *       model: dall-e-3
 * }</pre>
 */
@ConfigurationProperties(prefix = "language-learning.ai")
@Validated
@Getter
@Setter
public class AiProviderConfig {

    /**
     * LLM provider configuration for text generation.
     */
    @NotNull
    @Valid
    private LlmConfig llm = new LlmConfig();

    /**
     * Speech-to-text provider configuration.
     */
    @Valid
    private SpeechToTextConfig speechToText;

    /**
     * Text-to-speech provider configuration.
     */
    @Valid
    private TextToSpeechConfig textToSpeech;

    /**
     * Image generation provider configuration.
     */
    @Valid
    private ImageConfig image;

    /**
     * Base configuration for all AI providers.
     */
    @Getter
    @Setter
    public static class BaseProviderConfig {

        /**
         * Provider name (openai, anthropic, ollama, etc.)
         */
        @NotBlank
        private String provider;

        /**
         * API key for authentication.
         */
        @Nullable
        private String apiKey;

        /**
         * Base URL for the API (optional, for custom endpoints).
         */
        @Nullable
        private String baseUrl;

        /**
         * Request timeout duration.
         */
        private Duration timeout = Duration.ofSeconds(60);

        /**
         * Maximum retry attempts on failure.
         */
        private int maxRetries = 3;

        @Nonnull
        public Optional<String> getApiKey() {
            return Optional.ofNullable(apiKey);
        }

        @Nonnull
        public Optional<String> getBaseUrl() {
            return Optional.ofNullable(baseUrl);
        }
    }

    /**
     * Configuration specific to LLM providers.
     */
    @Getter
    @Setter
    public static class LlmConfig extends BaseProviderConfig {

        /**
         * Model name to use (e.g., gpt-4-turbo-preview, claude-3-opus).
         */
        @NotBlank
        private String model = "gpt-4-turbo-preview";

        /**
         * Temperature for response randomness (0.0 to 2.0).
         */
        private double temperature = 0.7;

        /**
         * Maximum tokens in the response.
         */
        private int maxTokens = 4096;

        public LlmConfig() {
            setProvider("openai");
        }
    }

    /**
     * Configuration specific to speech-to-text providers.
     */
    @Getter
    @Setter
    public static class SpeechToTextConfig extends BaseProviderConfig {

        /**
         * Model name for transcription.
         */
        private String model = "whisper-1";

        /**
         * Language hint for better transcription accuracy.
         */
        @Nullable
        private String language;

        public SpeechToTextConfig() {
            setProvider("openai");
        }

        @Nonnull
        public Optional<String> getLanguage() {
            return Optional.ofNullable(language);
        }
    }

    /**
     * Configuration specific to text-to-speech providers.
     */
    @Getter
    @Setter
    public static class TextToSpeechConfig extends BaseProviderConfig {

        /**
         * Model name for synthesis.
         */
        private String model = "tts-1";

        /**
         * Voice to use for synthesis.
         */
        private String voice = "alloy";

        /**
         * Speech speed (0.25 to 4.0).
         */
        private double speed = 1.0;

        public TextToSpeechConfig() {
            setProvider("openai");
        }
    }

    /**
     * Configuration specific to image generation providers.
     */
    @Getter
    @Setter
    public static class ImageConfig extends BaseProviderConfig {

        /**
         * Model name for image generation.
         */
        private String model = "dall-e-3";

        /**
         * Image size (e.g., 1024x1024, 512x512).
         */
        private String size = "1024x1024";

        /**
         * Image quality (standard, hd).
         */
        private String quality = "standard";

        /**
         * Image style (vivid, natural).
         */
        private String style = "natural";

        public ImageConfig() {
            setProvider("openai");
        }
    }

    /**
     * Checks if speech-to-text is configured.
     */
    public boolean hasSpeechToText() {
        return speechToText != null;
    }

    /**
     * Checks if text-to-speech is configured.
     */
    public boolean hasTextToSpeech() {
        return textToSpeech != null;
    }

    /**
     * Checks if image generation is configured.
     */
    public boolean hasImageGeneration() {
        return image != null;
    }
}