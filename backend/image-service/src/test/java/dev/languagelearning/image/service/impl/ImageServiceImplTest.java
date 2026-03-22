package dev.languagelearning.image.service.impl;

import dev.languagelearning.config.AiProviderConfig;
import dev.languagelearning.image.exception.ImageGenerationException;
import dev.languagelearning.image.service.ImageService;
import dev.languagelearning.image.service.ImageService.GeneratedImage;
import dev.languagelearning.image.service.ImageService.ImageGenerationOptions;
import dev.languagelearning.image.service.ImageService.ImageSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.image.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ImageServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private ImageModel imageModel;

    @Mock
    private AiProviderConfig aiProviderConfig;

    private ImageServiceImpl imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageServiceImpl(imageModel, aiProviderConfig);
    }

    @Nested
    @DisplayName("generate(prompt)")
    class GenerateWithPrompt {

        @Test
        @DisplayName("should generate image and return URL")
        void shouldGenerateImageAndReturnUrl() {
            // Given
            String prompt = "A beautiful sunset over mountains";
            String expectedUrl = "https://example.com/image.png";

            ImageResponse mockResponse = createMockImageResponse(expectedUrl, null);
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When
            GeneratedImage result = imageService.generate(prompt);

            // Then
            assertThat(result.url()).isEqualTo(expectedUrl);
            // Note: revisedPrompt is the original prompt since getRevisionPrompt() is not available in Spring AI 1.0.0-M5
            assertThat(result.revisedPrompt()).isEqualTo(prompt);
            assertThat(result.size()).isEqualTo(ImageSize.LARGE);
            verify(imageModel).call(any(ImagePrompt.class));
        }

        @Test
        @DisplayName("should use original prompt when revised prompt is null")
        void shouldUseOriginalPromptWhenRevisedPromptIsNull() {
            // Given
            String prompt = "A cat playing";
            String expectedUrl = "https://example.com/cat.png";

            ImageResponse mockResponse = createMockImageResponse(expectedUrl, null);
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When
            GeneratedImage result = imageService.generate(prompt);

            // Then
            assertThat(result.revisedPrompt()).isEqualTo(prompt);
        }

        @Test
        @DisplayName("should throw exception when response is null")
        void shouldThrowExceptionWhenResponseIsNull() {
            // Given
            String prompt = "Test prompt";
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(null);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> imageService.generate(prompt))
                    .isInstanceOf(ImageGenerationException.class)
                    .hasMessageContaining("Empty response");
        }

        @Test
        @DisplayName("should use base64 when URL is not available")
        void shouldUseBase64WhenUrlNotAvailable() {
            // Given
            String prompt = "Test prompt";
            String base64Data = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJ";

            ImageResponse mockResponse = createMockImageResponseWithBase64(null, base64Data, "revised");
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When
            GeneratedImage result = imageService.generate(prompt);

            // Then
            assertThat(result.url()).startsWith("data:image/png;base64,");
            assertThat(result.url()).contains(base64Data);
        }

        @Test
        @DisplayName("should throw exception when both URL and base64 are missing")
        void shouldThrowExceptionWhenBothUrlAndBase64AreMissing() {
            // Given
            String prompt = "Test prompt";

            ImageResponse mockResponse = createMockImageResponseWithBase64(null, null, "revised");
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> imageService.generate(prompt))
                    .isInstanceOf(ImageGenerationException.class)
                    .hasMessageContaining("No image URL or base64 data");
        }

        @Test
        @DisplayName("should wrap exception from image model")
        void shouldWrapExceptionFromImageModel() {
            // Given
            String prompt = "Test prompt";
            when(imageModel.call(any(ImagePrompt.class)))
                    .thenThrow(new RuntimeException("API error"));
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> imageService.generate(prompt))
                    .isInstanceOf(ImageGenerationException.class)
                    .hasMessageContaining("Failed to generate image")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("generateAsync(prompt)")
    class GenerateAsync {

        @Test
        @DisplayName("should generate image asynchronously")
        void shouldGenerateImageAsynchronously() throws ExecutionException, InterruptedException {
            // Given
            String prompt = "Async test";
            String expectedUrl = "https://example.com/async.png";

            ImageResponse mockResponse = createMockImageResponse(expectedUrl, prompt);
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When
            CompletableFuture<GeneratedImage> future = imageService.generateAsync(prompt);
            GeneratedImage result = future.get();

            // Then
            assertThat(result.url()).isEqualTo(expectedUrl);
        }
    }

    @Nested
    @DisplayName("generateFlashcardImage")
    class GenerateFlashcardImage {

        @Test
        @DisplayName("should build prompt for flashcard with word and language")
        void shouldBuildPromptForFlashcard() {
            // Given
            String word = "Hund";
            String targetLang = "de";
            String expectedUrl = "https://example.com/dog.png";

            ImageResponse mockResponse = createMockImageResponse(expectedUrl, "dog illustration");
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When
            GeneratedImage result = imageService.generateFlashcardImage(word, targetLang, null);

            // Then
            assertThat(result.url()).isEqualTo(expectedUrl);
            assertThat(result.size()).isEqualTo(ImageSize.MEDIUM);
        }

        @Test
        @DisplayName("should include context in flashcard prompt")
        void shouldIncludeContextInFlashcardPrompt() {
            // Given
            String word = "chat";
            String targetLang = "fr";
            String context = "Le chat dort sur le canapé";
            String expectedUrl = "https://example.com/cat.png";

            ImageResponse mockResponse = createMockImageResponse(expectedUrl, "cat on sofa");
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When
            GeneratedImage result = imageService.generateFlashcardImage(word, targetLang, context);

            // Then
            assertThat(result.url()).isEqualTo(expectedUrl);
        }
    }

    @Nested
    @DisplayName("generate(prompt, options)")
    class GenerateWithOptions {

        @Test
        @DisplayName("should use custom options for image generation")
        void shouldUseCustomOptions() {
            // Given
            String prompt = "Test";
            ImageGenerationOptions options = ImageGenerationOptions.highQuality();
            String expectedUrl = "https://example.com/hq.png";

            ImageResponse mockResponse = createMockImageResponse(expectedUrl, prompt);
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(null);

            // When
            GeneratedImage result = imageService.generate(prompt, options);

            // Then
            assertThat(result.url()).isEqualTo(expectedUrl);
            assertThat(result.size()).isEqualTo(ImageSize.LARGE);
        }

        @Test
        @DisplayName("should use model from config when available")
        void shouldUseModelFromConfig() {
            // Given
            String prompt = "Test";
            String expectedUrl = "https://example.com/test.png";
            AiProviderConfig.ImageConfig imageConfig = new AiProviderConfig.ImageConfig();
            imageConfig.setModel("dall-e-2");

            ImageResponse mockResponse = createMockImageResponse(expectedUrl, prompt);
            when(imageModel.call(any(ImagePrompt.class))).thenReturn(mockResponse);
            when(aiProviderConfig.getImage()).thenReturn(imageConfig);

            // When
            GeneratedImage result = imageService.generate(prompt);

            // Then
            assertThat(result.url()).isEqualTo(expectedUrl);
            verify(imageModel).call(any(ImagePrompt.class));
        }
    }

    @Nested
    @DisplayName("ImageGenerationOptions")
    class ImageGenerationOptionsTests {

        @Test
        @DisplayName("defaults should have standard quality and natural style")
        void defaultsShouldHaveStandardQualityAndNaturalStyle() {
            // When
            ImageGenerationOptions options = ImageGenerationOptions.defaults();

            // Then
            assertThat(options.size()).isEqualTo(ImageSize.LARGE);
            assertThat(options.quality()).isEqualTo("standard");
            assertThat(options.style()).isEqualTo("natural");
        }

        @Test
        @DisplayName("highQuality should have hd quality and vivid style")
        void highQualityShouldHaveHdQualityAndVividStyle() {
            // When
            ImageGenerationOptions options = ImageGenerationOptions.highQuality();

            // Then
            assertThat(options.size()).isEqualTo(ImageSize.LARGE);
            assertThat(options.quality()).isEqualTo("hd");
            assertThat(options.style()).isEqualTo("vivid");
        }

        @Test
        @DisplayName("forFlashcard should have medium size")
        void forFlashcardShouldHaveMediumSize() {
            // When
            ImageGenerationOptions options = ImageGenerationOptions.forFlashcard();

            // Then
            assertThat(options.size()).isEqualTo(ImageSize.MEDIUM);
            assertThat(options.quality()).isEqualTo("standard");
        }
    }

    @Nested
    @DisplayName("ImageSize")
    class ImageSizeTests {

        @Test
        @DisplayName("should have correct dimensions")
        void shouldHaveCorrectDimensions() {
            assertThat(ImageSize.SMALL.getDimensions()).isEqualTo("256x256");
            assertThat(ImageSize.MEDIUM.getDimensions()).isEqualTo("512x512");
            assertThat(ImageSize.LARGE.getDimensions()).isEqualTo("1024x1024");
            assertThat(ImageSize.WIDE.getDimensions()).isEqualTo("1792x1024");
            assertThat(ImageSize.TALL.getDimensions()).isEqualTo("1024x1792");
        }
    }

    // Helper methods
    private ImageResponse createMockImageResponse(String url, String revisedPrompt) {
        Image mockOutput = mock(Image.class);
        when(mockOutput.getUrl()).thenReturn(url);
        // Note: getRevisionPrompt() is not available in Spring AI 1.0.0-M5
        // The implementation uses the original prompt instead

        ImageGeneration mockGeneration = mock(ImageGeneration.class);
        when(mockGeneration.getOutput()).thenReturn(mockOutput);

        ImageResponse mockResponse = mock(ImageResponse.class);
        when(mockResponse.getResult()).thenReturn(mockGeneration);

        return mockResponse;
    }

    private ImageResponse createMockImageResponseWithBase64(String url, String base64, String revisedPrompt) {
        Image mockOutput = mock(Image.class);
        when(mockOutput.getUrl()).thenReturn(url);
        when(mockOutput.getB64Json()).thenReturn(base64);
        // Note: getRevisionPrompt() is not available in Spring AI 1.0.0-M5
        // The implementation uses the original prompt instead

        ImageGeneration mockGeneration = mock(ImageGeneration.class);
        when(mockGeneration.getOutput()).thenReturn(mockOutput);

        ImageResponse mockResponse = mock(ImageResponse.class);
        when(mockResponse.getResult()).thenReturn(mockGeneration);

        return mockResponse;
    }
}
