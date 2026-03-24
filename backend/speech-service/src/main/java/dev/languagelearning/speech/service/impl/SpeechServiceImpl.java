package dev.languagelearning.speech.service.impl;

import dev.languagelearning.config.AiProviderConfig;
import dev.languagelearning.speech.exception.SpeechException;
import dev.languagelearning.speech.service.SpeechService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of {@link SpeechService} using Spring AI.
 * <p>
 * Provides text-to-speech and speech-to-text functionality via OpenAI APIs.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(OpenAiAudioSpeechModel.class)
public class SpeechServiceImpl implements SpeechService {

    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final AiProviderConfig aiProviderConfig;

    @Override
    @Nonnull
    public byte[] textToSpeech(@Nonnull String text, @Nonnull String languageCode) {
        return textToSpeech(text, SpeechOptions.defaults(languageCode));
    }

    @Override
    @Nonnull
    public byte[] textToSpeech(@Nonnull String text, @Nonnull SpeechOptions options) {
        log.debug("Converting text to speech: {} chars, language: {}", text.length(), options.languageCode());

        try {
            var ttsConfig = aiProviderConfig.getTextToSpeech();

            OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                    .withModel(ttsConfig != null ? ttsConfig.getModel() : "tts-1")
                    .withVoice(OpenAiAudioApi.SpeechRequest.Voice.valueOf(options.voice().name()))
                    .withSpeed((float) options.speed())
                    .withResponseFormat(mapAudioFormat(options.format()))
                    .build();

            SpeechPrompt prompt = new SpeechPrompt(text, speechOptions);
            SpeechResponse response = speechModel.call(prompt);

            if (response == null || response.getResult() == null) {
                throw SpeechException.textToSpeechFailed("Empty response from speech model");
            }

            byte[] audioData = response.getResult().getOutput();
            if (audioData == null || audioData.length == 0) {
                throw SpeechException.textToSpeechFailed("No audio data in response");
            }

            log.info("Successfully converted {} chars to {} bytes of audio", text.length(), audioData.length);
            return audioData;

        } catch (SpeechException e) {
            throw e;
        } catch (Exception e) {
            log.error("Text-to-speech failed: {}", e.getMessage(), e);
            throw new SpeechException("Text-to-speech failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Nonnull
    public TranscriptionResult speechToText(@Nonnull byte[] audioData, @Nullable String languageHint) {
        log.debug("Transcribing {} bytes of audio, language hint: {}", audioData.length, languageHint);

        try {
            Resource audioResource = new ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return "audio.wav";
                }
            };

            return transcribe(audioResource, languageHint);

        } catch (SpeechException e) {
            throw e;
        } catch (Exception e) {
            log.error("Speech-to-text failed: {}", e.getMessage(), e);
            throw new SpeechException("Speech-to-text failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Nonnull
    public TranscriptionResult speechToText(@Nonnull InputStream audioStream, @Nullable String languageHint) {
        log.debug("Transcribing audio from stream, language hint: {}", languageHint);

        try {
            byte[] audioData = audioStream.readAllBytes();
            return speechToText(audioData, languageHint);

        } catch (IOException e) {
            log.error("Failed to read audio stream: {}", e.getMessage(), e);
            throw new SpeechException("Failed to read audio stream: " + e.getMessage(), e);
        }
    }

    private TranscriptionResult transcribe(Resource audioResource, String languageHint) {
        var sttConfig = aiProviderConfig.getSpeechToText();

        OpenAiAudioTranscriptionOptions.Builder optionsBuilder = OpenAiAudioTranscriptionOptions.builder()
                .withModel(sttConfig != null ? sttConfig.getModel() : "whisper-1")
                .withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON);

        if (languageHint != null && !languageHint.isBlank()) {
            optionsBuilder.withLanguage(languageHint);
        } else if (sttConfig != null && sttConfig.getLanguage().isPresent()) {
            optionsBuilder.withLanguage(sttConfig.getLanguage().get());
        }

        String text = transcriptionModel.call(audioResource);

        if (text == null || text.isBlank()) {
            throw SpeechException.transcriptionFailed("Empty transcription result");
        }

        log.info("Successfully transcribed audio to {} chars", text.length());
        return TranscriptionResult.of(text.trim(), languageHint);
    }

    private OpenAiAudioApi.SpeechRequest.AudioResponseFormat mapAudioFormat(AudioFormat format) {
        return switch (format) {
            case MP3 -> OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3;
            case OPUS -> OpenAiAudioApi.SpeechRequest.AudioResponseFormat.OPUS;
            case AAC -> OpenAiAudioApi.SpeechRequest.AudioResponseFormat.AAC;
            case FLAC -> OpenAiAudioApi.SpeechRequest.AudioResponseFormat.FLAC;
            case WAV -> OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3;
        };
    }
}