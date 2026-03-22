package dev.languagelearning.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AiProviderConfig}.
 */
class AiProviderConfigTest {

    private AiProviderConfig config;

    @BeforeEach
    void setUp() {
        config = new AiProviderConfig();
    }

    @Nested
    @DisplayName("LlmConfig")
    class LlmConfigTests {

        @Test
        @DisplayName("should have default values")
        void shouldHaveDefaultValues() {
            // Given
            AiProviderConfig.LlmConfig llmConfig = new AiProviderConfig.LlmConfig();

            // Then
            assertThat(llmConfig.getProvider()).isEqualTo("openai");
            assertThat(llmConfig.getModel()).isEqualTo("gpt-4-turbo-preview");
            assertThat(llmConfig.getTemperature()).isEqualTo(0.7);
            assertThat(llmConfig.getMaxTokens()).isEqualTo(4096);
        }

        @Test
        @DisplayName("should allow setting custom values")
        void shouldAllowSettingCustomValues() {
            // Given
            AiProviderConfig.LlmConfig llmConfig = new AiProviderConfig.LlmConfig();

            // When
            llmConfig.setProvider("anthropic");
            llmConfig.setModel("claude-3-opus");
            llmConfig.setTemperature(0.5);
            llmConfig.setMaxTokens(8192);
            llmConfig.setApiKey("test-api-key");
            llmConfig.setBaseUrl("https://custom.api.com");

            // Then
            assertThat(llmConfig.getProvider()).isEqualTo("anthropic");
            assertThat(llmConfig.getModel()).isEqualTo("claude-3-opus");
            assertThat(llmConfig.getTemperature()).isEqualTo(0.5);
            assertThat(llmConfig.getMaxTokens()).isEqualTo(8192);
            assertThat(llmConfig.getApiKey()).isPresent().contains("test-api-key");
            assertThat(llmConfig.getBaseUrl()).isPresent().contains("https://custom.api.com");
        }
    }

    @Nested
    @DisplayName("SpeechToTextConfig")
    class SpeechToTextConfigTests {

        @Test
        @DisplayName("should have default values")
        void shouldHaveDefaultValues() {
            // Given
            AiProviderConfig.SpeechToTextConfig sttConfig = new AiProviderConfig.SpeechToTextConfig();

            // Then
            assertThat(sttConfig.getProvider()).isEqualTo("openai");
            assertThat(sttConfig.getModel()).isEqualTo("whisper-1");
            assertThat(sttConfig.getLanguage()).isEmpty();
        }

        @Test
        @DisplayName("should support language hint")
        void shouldSupportLanguageHint() {
            // Given
            AiProviderConfig.SpeechToTextConfig sttConfig = new AiProviderConfig.SpeechToTextConfig();

            // When
            sttConfig.setLanguage("de");

            // Then
            assertThat(sttConfig.getLanguage()).isPresent().contains("de");
        }
    }

    @Nested
    @DisplayName("TextToSpeechConfig")
    class TextToSpeechConfigTests {

        @Test
        @DisplayName("should have default values")
        void shouldHaveDefaultValues() {
            // Given
            AiProviderConfig.TextToSpeechConfig ttsConfig = new AiProviderConfig.TextToSpeechConfig();

            // Then
            assertThat(ttsConfig.getProvider()).isEqualTo("openai");
            assertThat(ttsConfig.getModel()).isEqualTo("tts-1");
            assertThat(ttsConfig.getVoice()).isEqualTo("alloy");
            assertThat(ttsConfig.getSpeed()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should allow customization")
        void shouldAllowCustomization() {
            // Given
            AiProviderConfig.TextToSpeechConfig ttsConfig = new AiProviderConfig.TextToSpeechConfig();

            // When
            ttsConfig.setVoice("shimmer");
            ttsConfig.setSpeed(1.25);
            ttsConfig.setModel("tts-1-hd");

            // Then
            assertThat(ttsConfig.getVoice()).isEqualTo("shimmer");
            assertThat(ttsConfig.getSpeed()).isEqualTo(1.25);
            assertThat(ttsConfig.getModel()).isEqualTo("tts-1-hd");
        }
    }

    @Nested
    @DisplayName("ImageConfig")
    class ImageConfigTests {

        @Test
        @DisplayName("should have default values")
        void shouldHaveDefaultValues() {
            // Given
            AiProviderConfig.ImageConfig imageConfig = new AiProviderConfig.ImageConfig();

            // Then
            assertThat(imageConfig.getProvider()).isEqualTo("openai");
            assertThat(imageConfig.getModel()).isEqualTo("dall-e-3");
            assertThat(imageConfig.getSize()).isEqualTo("1024x1024");
            assertThat(imageConfig.getQuality()).isEqualTo("standard");
            assertThat(imageConfig.getStyle()).isEqualTo("natural");
        }

        @Test
        @DisplayName("should allow HD quality and vivid style")
        void shouldAllowHDQualityAndVividStyle() {
            // Given
            AiProviderConfig.ImageConfig imageConfig = new AiProviderConfig.ImageConfig();

            // When
            imageConfig.setQuality("hd");
            imageConfig.setStyle("vivid");
            imageConfig.setSize("1792x1024");

            // Then
            assertThat(imageConfig.getQuality()).isEqualTo("hd");
            assertThat(imageConfig.getStyle()).isEqualTo("vivid");
            assertThat(imageConfig.getSize()).isEqualTo("1792x1024");
        }
    }

    @Nested
    @DisplayName("BaseProviderConfig")
    class BaseProviderConfigTests {

        @Test
        @DisplayName("should have default timeout of 60 seconds")
        void shouldHaveDefaultTimeout() {
            // Given
            AiProviderConfig.LlmConfig llmConfig = new AiProviderConfig.LlmConfig();

            // Then
            assertThat(llmConfig.getTimeout()).isEqualTo(Duration.ofSeconds(60));
        }

        @Test
        @DisplayName("should have default max retries of 3")
        void shouldHaveDefaultMaxRetries() {
            // Given
            AiProviderConfig.LlmConfig llmConfig = new AiProviderConfig.LlmConfig();

            // Then
            assertThat(llmConfig.getMaxRetries()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return empty Optional for null API key")
        void shouldReturnEmptyOptionalForNullApiKey() {
            // Given
            AiProviderConfig.LlmConfig llmConfig = new AiProviderConfig.LlmConfig();
            llmConfig.setApiKey(null);

            // Then
            assertThat(llmConfig.getApiKey()).isEmpty();
        }

        @Test
        @DisplayName("should return empty Optional for null base URL")
        void shouldReturnEmptyOptionalForNullBaseUrl() {
            // Given
            AiProviderConfig.LlmConfig llmConfig = new AiProviderConfig.LlmConfig();
            llmConfig.setBaseUrl(null);

            // Then
            assertThat(llmConfig.getBaseUrl()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Feature availability checks")
    class FeatureAvailabilityChecks {

        @Test
        @DisplayName("hasSpeechToText should return false when not configured")
        void hasSpeechToTextShouldReturnFalseWhenNotConfigured() {
            // Given
            config.setSpeechToText(null);

            // Then
            assertThat(config.hasSpeechToText()).isFalse();
        }

        @Test
        @DisplayName("hasSpeechToText should return true when configured")
        void hasSpeechToTextShouldReturnTrueWhenConfigured() {
            // Given
            config.setSpeechToText(new AiProviderConfig.SpeechToTextConfig());

            // Then
            assertThat(config.hasSpeechToText()).isTrue();
        }

        @Test
        @DisplayName("hasTextToSpeech should return false when not configured")
        void hasTextToSpeechShouldReturnFalseWhenNotConfigured() {
            // Given
            config.setTextToSpeech(null);

            // Then
            assertThat(config.hasTextToSpeech()).isFalse();
        }

        @Test
        @DisplayName("hasTextToSpeech should return true when configured")
        void hasTextToSpeechShouldReturnTrueWhenConfigured() {
            // Given
            config.setTextToSpeech(new AiProviderConfig.TextToSpeechConfig());

            // Then
            assertThat(config.hasTextToSpeech()).isTrue();
        }

        @Test
        @DisplayName("hasImageGeneration should return false when not configured")
        void hasImageGenerationShouldReturnFalseWhenNotConfigured() {
            // Given
            config.setImage(null);

            // Then
            assertThat(config.hasImageGeneration()).isFalse();
        }

        @Test
        @DisplayName("hasImageGeneration should return true when configured")
        void hasImageGenerationShouldReturnTrueWhenConfigured() {
            // Given
            config.setImage(new AiProviderConfig.ImageConfig());

            // Then
            assertThat(config.hasImageGeneration()).isTrue();
        }
    }

    @Nested
    @DisplayName("Default LLM config")
    class DefaultLlmConfig {

        @Test
        @DisplayName("should have LLM config by default")
        void shouldHaveLlmConfigByDefault() {
            // Then
            assertThat(config.getLlm()).isNotNull();
        }
    }
}