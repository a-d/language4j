package dev.languagelearning.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LanguageConfig}.
 */
class LanguageConfigTest {

    private LanguageConfig config;

    @BeforeEach
    void setUp() {
        config = new LanguageConfig();
    }

    @Nested
    @DisplayName("Language pair")
    class LanguagePair {

        @Test
        @DisplayName("should create language pair string from native and target")
        void shouldCreateLanguagePairString() {
            // Given
            config.setNativeCode("de");
            config.setTargetCode("fr");

            // When
            String pair = config.getLanguagePair();

            // Then
            assertThat(pair).isEqualTo("de-fr");
        }

        @Test
        @DisplayName("should handle English to Spanish")
        void shouldHandleEnglishToSpanish() {
            // Given
            config.setNativeCode("en");
            config.setTargetCode("es");

            // When
            String pair = config.getLanguagePair();

            // Then
            assertThat(pair).isEqualTo("en-es");
        }

        @Test
        @DisplayName("should handle same language pair")
        void shouldHandleSameLanguagePair() {
            // Given
            config.setNativeCode("en");
            config.setTargetCode("en");

            // When
            String pair = config.getLanguagePair();

            // Then
            assertThat(pair).isEqualTo("en-en");
        }
    }

    @Nested
    @DisplayName("Setters and getters")
    class SettersAndGetters {

        @Test
        @DisplayName("should store and retrieve native code")
        void shouldStoreAndRetrieveNativeCode() {
            // When
            config.setNativeCode("ja");

            // Then
            assertThat(config.getNativeCode()).isEqualTo("ja");
        }

        @Test
        @DisplayName("should store and retrieve target code")
        void shouldStoreAndRetrieveTargetCode() {
            // When
            config.setTargetCode("zh");

            // Then
            assertThat(config.getTargetCode()).isEqualTo("zh");
        }
    }
}