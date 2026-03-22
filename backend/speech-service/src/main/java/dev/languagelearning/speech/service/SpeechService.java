package dev.languagelearning.speech.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.InputStream;

/**
 * Main speech service that combines text-to-speech and speech-to-text capabilities.
 * <p>
 * Provides unified access to speech processing features for language learning.
 */
public interface SpeechService {

    /**
     * Converts text to speech audio.
     *
     * @param text         the text to convert to speech
     * @param languageCode the language code (ISO 639-1)
     * @return audio data as byte array
     */
    @Nonnull
    byte[] textToSpeech(@Nonnull String text, @Nonnull String languageCode);

    /**
     * Converts text to speech with custom options.
     *
     * @param text    the text to convert
     * @param options the speech synthesis options
     * @return audio data as byte array
     */
    @Nonnull
    byte[] textToSpeech(@Nonnull String text, @Nonnull SpeechOptions options);

    /**
     * Transcribes audio to text.
     *
     * @param audioData    the audio data to transcribe
     * @param languageHint optional language hint (ISO 639-1)
     * @return transcription result
     */
    @Nonnull
    TranscriptionResult speechToText(@Nonnull byte[] audioData, @Nullable String languageHint);

    /**
     * Transcribes audio from an input stream.
     *
     * @param audioStream  the audio input stream
     * @param languageHint optional language hint
     * @return transcription result
     */
    @Nonnull
    TranscriptionResult speechToText(@Nonnull InputStream audioStream, @Nullable String languageHint);

    /**
     * Speech synthesis options.
     */
    record SpeechOptions(
            @Nonnull String languageCode,
            @Nonnull Voice voice,
            double speed,
            @Nonnull AudioFormat format
    ) {
        public static SpeechOptions defaults(String languageCode) {
            return new SpeechOptions(languageCode, Voice.ALLOY, 1.0, AudioFormat.MP3);
        }

        public static SpeechOptions slow(String languageCode) {
            return new SpeechOptions(languageCode, Voice.ALLOY, 0.75, AudioFormat.MP3);
        }

        public static SpeechOptions withVoice(String languageCode, Voice voice) {
            return new SpeechOptions(languageCode, voice, 1.0, AudioFormat.MP3);
        }
    }

    /**
     * Available voice options.
     */
    enum Voice {
        ALLOY("alloy"),
        ECHO("echo"),
        FABLE("fable"),
        ONYX("onyx"),
        NOVA("nova"),
        SHIMMER("shimmer");

        private final String id;

        Voice(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Audio format options.
     */
    enum AudioFormat {
        MP3("mp3", "audio/mpeg"),
        OPUS("opus", "audio/opus"),
        AAC("aac", "audio/aac"),
        FLAC("flac", "audio/flac"),
        WAV("wav", "audio/wav");

        private final String extension;
        private final String mimeType;

        AudioFormat(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }

        public String getExtension() {
            return extension;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    /**
     * Result of speech-to-text transcription.
     */
    record TranscriptionResult(
            @Nonnull String text,
            @Nullable String detectedLanguage,
            double confidence
    ) {
        public static TranscriptionResult of(String text) {
            return new TranscriptionResult(text, null, 1.0);
        }

        public static TranscriptionResult of(String text, String language) {
            return new TranscriptionResult(text, language, 1.0);
        }

        public static TranscriptionResult of(String text, String language, double confidence) {
            return new TranscriptionResult(text, language, confidence);
        }
    }
}