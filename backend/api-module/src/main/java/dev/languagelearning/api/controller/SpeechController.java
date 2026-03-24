package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.ErrorResponse;
import dev.languagelearning.speech.service.SpeechService;
import dev.languagelearning.speech.service.SpeechOptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST controller for speech-related operations.
 * <p>
 * Provides text-to-speech (TTS) and speech-to-text (STT) functionality
 * for pronunciation practice and listening exercises.
 */
@RestController
@RequestMapping("/api/v1/speech")
@RequiredArgsConstructor
@Tag(name = "Speech", description = "Text-to-speech and speech-to-text services")
public class SpeechController {

    private final SpeechService speechService;

    /**
     * Synthesize text to speech audio.
     */
    @PostMapping(value = "/synthesize", produces = "audio/mpeg")
    @Operation(
            summary = "Text to speech",
            description = "Converts text to speech audio. Returns MP3 audio data. Use the 'slow' parameter for learning-friendly slower speech."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audio generated successfully",
                    content = @Content(mediaType = "audio/mpeg")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Speech synthesis failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<byte[]> synthesize(
            @RequestBody SynthesizeRequest request
    ) {
        SpeechOptions options = request.slow()
                ? SpeechOptions.slow(request.languageCode())
                : SpeechOptions.defaults(request.languageCode());
        
        if (request.voice() != null) {
            options = SpeechOptions.withVoice(request.languageCode(), request.voice());
        }
        
        byte[] audio = speechService.textToSpeech(request.text(), options);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }

    /**
     * Transcribe speech audio to text.
     */
    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Speech to text",
            description = "Transcribes audio to text. Accepts audio files (MP3, WAV, WebM, etc.). The language hint improves transcription accuracy."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audio transcribed successfully",
                    content = @Content(schema = @Schema(implementation = TranscriptionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid audio file",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Transcription failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<TranscriptionResponse> transcribe(
            @Parameter(description = "Audio file to transcribe")
            @RequestParam("audio") MultipartFile audioFile,
            @Parameter(description = "ISO 639-1 language code hint for better accuracy")
            @RequestParam(value = "languageHint", required = false) String languageHint
    ) throws IOException {
        String transcription = speechService.speechToText(audioFile.getBytes(), languageHint);
        return ResponseEntity.ok(new TranscriptionResponse(transcription));
    }

    // ==================== Request/Response DTOs ====================

    @Schema(description = "Request to synthesize text to speech")
    public record SynthesizeRequest(
            @Schema(description = "Text to convert to speech", example = "Bonjour, comment allez-vous?", requiredMode = Schema.RequiredMode.REQUIRED)
            String text,

            @Schema(description = "ISO 639-1 language code", example = "fr", requiredMode = Schema.RequiredMode.REQUIRED)
            String languageCode,

            @Schema(description = "Use slower speech speed for learning", example = "true", defaultValue = "false")
            boolean slow,

            @Schema(description = "Voice selection (ALLOY, ECHO, FABLE, ONYX, NOVA, SHIMMER)", example = "NOVA", nullable = true)
            String voice
    ) {
        public SynthesizeRequest {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Text is required");
            }
            if (languageCode == null || languageCode.isBlank()) {
                throw new IllegalArgumentException("Language code is required");
            }
        }
    }

    @Schema(description = "Transcription result")
    public record TranscriptionResponse(
            @Schema(description = "Transcribed text from audio", example = "Bonjour, comment allez-vous?")
            String transcription
    ) {}
}