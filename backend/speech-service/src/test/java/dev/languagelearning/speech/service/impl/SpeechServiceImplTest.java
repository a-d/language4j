package dev.languagelearning.speech.service.impl;

import dev.languagelearning.config.AiProviderConfig;
import dev.languagelearning.speech.exception.SpeechException;
import dev.languagelearning.speech.service.SpeechService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.audio.speech.Speech;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SpeechServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class SpeechServiceImplTest {

    @Mock
    private OpenAiAudioSpeechModel speechModel;

    @Mock
    private OpenAiAudioTranscriptionModel transcriptionModel;

    @Mock
    private AiProviderConfig aiProviderConfig;

    private SpeechServiceImpl speechService;

    @BeforeEach
    void setUp() {
        speechService = new SpeechServiceImpl(speechModel, transcriptionModel, aiProviderConfig);
    }

    @Nested
    @DisplayName("textToSpeech(text, languageCode)")
    class TextToSpeechWithLanguageCode {

        @Test
        @DisplayName("should convert text to speech audio")
        void shouldConvertTextToSpeechAudio() {
            // Given
            String text = "Hello, world!";
            String languageCode = "en";
            byte[] expectedAudio = new byte[]{1, 2, 3, 4, 5};

            SpeechResponse mockResponse = createMockSpeechResponse(expectedAudio);
            when(speechModel.call(any(SpeechPrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getTextToSpeech()).thenReturn(null);

            // When
            byte[] result = speechService.textToSpeech(text, languageCode);

            // Then
            assertThat(result).isEqualTo(expectedAudio);
            verify(speechModel).call(any(SpeechPrompt.class));
        }

        @Test
        @DisplayName("should throw exception when response is null")
        void shouldThrowExceptionWhenResponseIsNull() {
            // Given
            String text = "Test";
            String languageCode = "en";
            when(speechModel.call(any(SpeechPrompt.class))).thenReturn(null);
            when(aiProviderConfig.getTextToSpeech()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> speechService.textToSpeech(text, languageCode))
                    .isInstanceOf(SpeechException.class)
                    .hasMessageContaining("Text-to-speech failed");
        }

        @Test
        @DisplayName("should throw exception when audio data is empty")
        void shouldThrowExceptionWhenAudioDataIsEmpty() {
            // Given
            String text = "Test";
            String languageCode = "en";
            SpeechResponse mockResponse = createMockSpeechResponse(new byte[0]);
            when(speechModel.call(any(SpeechPrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getTextToSpeech()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> speechService.textToSpeech(text, languageCode))
                    .isInstanceOf(SpeechException.class)
                    .hasMessageContaining("No audio data");
        }
    }

    @Nested
    @DisplayName("textToSpeech(text, options)")
    class TextToSpeechWithOptions {

        @Test
        @DisplayName("should use custom voice option")
        void shouldUseCustomVoiceOption() {
            // Given
            String text = "Bonjour";
            SpeechOptions options = SpeechOptions.withVoice("fr", Voice.NOVA);
            byte[] expectedAudio = new byte[]{10, 20, 30};

            SpeechResponse mockResponse = createMockSpeechResponse(expectedAudio);
            when(speechModel.call(any(SpeechPrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getTextToSpeech()).thenReturn(null);

            // When
            byte[] result = speechService.textToSpeech(text, options);

            // Then
            assertThat(result).isEqualTo(expectedAudio);
        }

        @Test
        @DisplayName("should use slow speed option")
        void shouldUseSlowSpeedOption() {
            // Given
            String text = "Slow speech";
            SpeechOptions options = SpeechOptions.slow("de");
            byte[] expectedAudio = new byte[]{5, 6, 7};

            SpeechResponse mockResponse = createMockSpeechResponse(expectedAudio);
            when(speechModel.call(any(SpeechPrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getTextToSpeech()).thenReturn(null);

            // When
            byte[] result = speechService.textToSpeech(text, options);

            // Then
            assertThat(result).isEqualTo(expectedAudio);
        }

        @Test
        @DisplayName("should use model from config when available")
        void shouldUseModelFromConfig() {
            // Given
            String text = "Test";
            SpeechOptions options = SpeechOptions.defaults("en");
            byte[] expectedAudio = new byte[]{1, 2, 3};

            AiProviderConfig.TextToSpeechConfig ttsConfig = new AiProviderConfig.TextToSpeechConfig();
            ttsConfig.setModel("tts-1-hd");

            SpeechResponse mockResponse = createMockSpeechResponse(expectedAudio);
            when(speechModel.call(any(SpeechPrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getTextToSpeech()).thenReturn(ttsConfig);

            // When
            byte[] result = speechService.textToSpeech(text, options);

            // Then
            assertThat(result).isEqualTo(expectedAudio);
        }

        @Test
        @DisplayName("should wrap exception from speech model")
        void shouldWrapExceptionFromSpeechModel() {
            // Given
            String text = "Test";
            SpeechOptions options = SpeechOptions.defaults("en");
            when(speechModel.call(any(SpeechPrompt.class)))
                    .thenThrow(new RuntimeException("API error"));
            when(aiProviderConfig.getTextToSpeech()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> speechService.textToSpeech(text, options))
                    .isInstanceOf(SpeechException.class)
                    .hasMessageContaining("Text-to-speech failed")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("speechToText(audioData, languageHint)")
    class SpeechToTextWithBytes {

        @Test
        @DisplayName("should transcribe audio to text")
        void shouldTranscribeAudioToText() {
            // Given
            byte[] audioData = new byte[]{1, 2, 3, 4, 5};
            String languageHint = "en";
            String expectedText = "Hello, world!";

            when(transcriptionModel.call(any(Resource.class))).thenReturn(expectedText);
            when(aiProviderConfig.getSpeechToText()).thenReturn(null);

            // When
            TranscriptionResult result = speechService.speechToText(audioData, languageHint);

            // Then
            assertThat(result.text()).isEqualTo(expectedText);
            assertThat(result.detectedLanguage()).isEqualTo(languageHint);
        }

        @Test
        @DisplayName("should transcribe without language hint")
        void shouldTranscribeWithoutLanguageHint() {
            // Given
            byte[] audioData = new byte[]{10, 20, 30};
            String expectedText = "Transcribed text";

            when(transcriptionModel.call(any(Resource.class))).thenReturn(expectedText);
            when(aiProviderConfig.getSpeechToText()).thenReturn(null);

            // When
            TranscriptionResult result = speechService.speechToText(audioData, null);

            // Then
            assertThat(result.text()).isEqualTo(expectedText);
            assertThat(result.detectedLanguage()).isNull();
        }

        @Test
        @DisplayName("should throw exception when transcription is empty")
        void shouldThrowExceptionWhenTranscriptionIsEmpty() {
            // Given
            byte[] audioData = new byte[]{1, 2, 3};
            when(transcriptionModel.call(any(Resource.class))).thenReturn("");
            when(aiProviderConfig.getSpeechToText()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> speechService.speechToText(audioData, null))
                    .isInstanceOf(SpeechException.class)
                    .hasMessageContaining("Empty transcription");
        }

        @Test
        @DisplayName("should throw exception when response is null")
        void shouldThrowExceptionWhenResponseIsNull() {
            // Given
            byte[] audioData = new byte[]{1, 2, 3};
            when(transcriptionModel.call(any(Resource.class))).thenReturn(null);
            when(aiProviderConfig.getSpeechToText()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> speechService.speechToText(audioData, null))
                    .isInstanceOf(SpeechException.class)
                    .hasMessageContaining("Empty transcription");
        }

        @Test
        @DisplayName("should use language from config when no hint provided")
        void shouldUseLanguageFromConfigWhenNoHint() {
            // Given
            byte[] audioData = new byte[]{1, 2, 3};
            String expectedText = "Transcribed";

            AiProviderConfig.SpeechToTextConfig sttConfig = new AiProviderConfig.SpeechToTextConfig();
            sttConfig.setLanguage("de");

            when(transcriptionModel.call(any(Resource.class))).thenReturn(expectedText);
            when(aiProviderConfig.getSpeechToText()).thenReturn(sttConfig);

            // When
            TranscriptionResult result = speechService.speechToText(audioData, null);

            // Then
            assertThat(result.text()).isEqualTo(expectedText);
        }
    }

    @Nested
    @DisplayName("speechToText(audioStream, languageHint)")
    class SpeechToTextWithStream {

        @Test
        @DisplayName("should transcribe audio from input stream")
        void shouldTranscribeAudioFromInputStream() {
            // Given
            byte[] audioData = new byte[]{1, 2, 3, 4, 5};
            InputStream audioStream = new ByteArrayInputStream(audioData);
            String expectedText = "Stream transcription";

            when(transcriptionModel.call(any(Resource.class))).thenReturn(expectedText);
            when(aiProviderConfig.getSpeechToText()).thenReturn(null);

            // When
            TranscriptionResult result = speechService.speechToText(audioStream, "en");

            // Then
            assertThat(result.text()).isEqualTo(expectedText);
        }

        @Test
        @DisplayName("should throw exception when stream read fails")
        void shouldThrowExceptionWhenStreamReadFails() throws IOException {
            // Given
            InputStream mockStream = mock(InputStream.class);
            when(mockStream.readAllBytes()).thenThrow(new IOException("Read error"));

            // When/Then
            assertThatThrownBy(() -> speechService.speechToText(mockStream, null))
                    .isInstanceOf(SpeechException.class)
                    .hasMessageContaining("Failed to read audio stream");
        }
    }

    @Nested
    @DisplayName("SpeechOptions")
    class SpeechOptionsTests {

        @Test
        @DisplayName("defaults should have ALLOY voice and speed 1.0")
        void defaultsShouldHaveAlloyVoiceAndNormalSpeed() {
            // When
            SpeechOptions options = SpeechOptions.defaults("en");

            // Then
            assertThat(options.languageCode()).isEqualTo("en");
            assertThat(options.voice()).isEqualTo(Voice.ALLOY);
            assertThat(options.speed()).isEqualTo(1.0);
            assertThat(options.format()).isEqualTo(AudioFormat.MP3);
        }

        @Test
        @DisplayName("slow should have speed 0.75")
        void slowShouldHaveReducedSpeed() {
            // When
            SpeechOptions options = SpeechOptions.slow("fr");

            // Then
            assertThat(options.speed()).isEqualTo(0.75);
        }

        @Test
        @DisplayName("withVoice should set custom voice")
        void withVoiceShouldSetCustomVoice() {
            // When
            SpeechOptions options = SpeechOptions.withVoice("de", Voice.SHIMMER);

            // Then
            assertThat(options.voice()).isEqualTo(Voice.SHIMMER);
        }
    }

    @Nested
    @DisplayName("Voice enum")
    class VoiceEnumTests {

        @Test
        @DisplayName("should have correct IDs")
        void shouldHaveCorrectIds() {
            assertThat(Voice.ALLOY.getId()).isEqualTo("alloy");
            assertThat(Voice.ECHO.getId()).isEqualTo("echo");
            assertThat(Voice.FABLE.getId()).isEqualTo("fable");
            assertThat(Voice.ONYX.getId()).isEqualTo("onyx");
            assertThat(Voice.NOVA.getId()).isEqualTo("nova");
            assertThat(Voice.SHIMMER.getId()).isEqualTo("shimmer");
        }
    }

    @Nested
    @DisplayName("AudioFormat enum")
    class AudioFormatEnumTests {

        @Test
        @DisplayName("should have correct extensions")
        void shouldHaveCorrectExtensions() {
            assertThat(AudioFormat.MP3.getExtension()).isEqualTo("mp3");
            assertThat(AudioFormat.OPUS.getExtension()).isEqualTo("opus");
            assertThat(AudioFormat.AAC.getExtension()).isEqualTo("aac");
            assertThat(AudioFormat.FLAC.getExtension()).isEqualTo("flac");
            assertThat(AudioFormat.WAV.getExtension()).isEqualTo("wav");
        }

        @Test
        @DisplayName("should have correct MIME types")
        void shouldHaveCorrectMimeTypes() {
            assertThat(AudioFormat.MP3.getMimeType()).isEqualTo("audio/mpeg");
            assertThat(AudioFormat.OPUS.getMimeType()).isEqualTo("audio/opus");
            assertThat(AudioFormat.AAC.getMimeType()).isEqualTo("audio/aac");
            assertThat(AudioFormat.FLAC.getMimeType()).isEqualTo("audio/flac");
            assertThat(AudioFormat.WAV.getMimeType()).isEqualTo("audio/wav");
        }
    }

    @Nested
    @DisplayName("TranscriptionResult")
    class TranscriptionResultTests {

        @Test
        @DisplayName("of(text) should create result with text only")
        void ofTextShouldCreateResultWithTextOnly() {
            // When
            TranscriptionResult result = TranscriptionResult.of("Hello");

            // Then
            assertThat(result.text()).isEqualTo("Hello");
            assertThat(result.detectedLanguage()).isNull();
            assertThat(result.confidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("of(text, language) should create result with language")
        void ofTextLanguageShouldCreateResultWithLanguage() {
            // When
            TranscriptionResult result = TranscriptionResult.of("Bonjour", "fr");

            // Then
            assertThat(result.text()).isEqualTo("Bonjour");
            assertThat(result.detectedLanguage()).isEqualTo("fr");
            assertThat(result.confidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("of(text, language, confidence) should create full result")
        void ofTextLanguageConfidenceShouldCreateFullResult() {
            // When
            TranscriptionResult result = TranscriptionResult.of("Hallo", "de", 0.95);

            // Then
            assertThat(result.text()).isEqualTo("Hallo");
            assertThat(result.detectedLanguage()).isEqualTo("de");
            assertThat(result.confidence()).isEqualTo(0.95);
        }
    }

    // Helper methods
    private SpeechResponse createMockSpeechResponse(byte[] audioData) {
        Speech mockSpeech = mock(Speech.class);
        when(mockSpeech.getOutput()).thenReturn(audioData);

        SpeechResponse mockResponse = mock(SpeechResponse.class);
        when(mockResponse.getResult()).thenReturn(mockSpeech);

        return mockResponse;
    }
}