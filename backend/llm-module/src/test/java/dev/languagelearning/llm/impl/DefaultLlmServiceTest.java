package dev.languagelearning.llm.impl;

import dev.languagelearning.llm.PromptTemplate;
import dev.languagelearning.llm.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultLlmService}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultLlmServiceTest {

    @Mock
    private ChatModel chatModel;

    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    private DefaultLlmService llmService;

    @BeforeEach
    void setUp() {
        llmService = new DefaultLlmService(chatModel);
    }

    @Nested
    @DisplayName("generate(userPrompt)")
    class GenerateWithUserPrompt {

        @Test
        @DisplayName("should return content from chat model response")
        void shouldReturnContentFromChatModelResponse() {
            // Given
            String userPrompt = "Translate 'Hello' to French";
            String expectedResponse = "Bonjour";
            mockChatResponse(expectedResponse);

            // When
            String result = llmService.generate(userPrompt);

            // Then
            assertThat(result).isEqualTo(expectedResponse);
            verify(chatModel).call(promptCaptor.capture());
            assertThat(promptCaptor.getValue().getContents()).contains(userPrompt);
        }

        @Test
        @DisplayName("should throw LlmException when response is null")
        void shouldThrowLlmExceptionWhenResponseIsNull() {
            // Given
            when(chatModel.call(any(Prompt.class))).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> llmService.generate("test prompt"))
                    .isInstanceOf(LlmException.class)
                    .hasMessageContaining("Empty response");
        }

        @Test
        @DisplayName("should throw LlmException when result is null")
        void shouldThrowLlmExceptionWhenResultIsNull() {
            // Given
            ChatResponse response = mock(ChatResponse.class);
            when(response.getResult()).thenReturn(null);
            when(chatModel.call(any(Prompt.class))).thenReturn(response);

            // When/Then
            assertThatThrownBy(() -> llmService.generate("test prompt"))
                    .isInstanceOf(LlmException.class)
                    .hasMessageContaining("Empty response");
        }

        @Test
        @DisplayName("should throw LlmException when output is null")
        void shouldThrowLlmExceptionWhenOutputIsNull() {
            // Given
            Generation generation = mock(Generation.class);
            when(generation.getOutput()).thenReturn(null);
            ChatResponse response = mock(ChatResponse.class);
            when(response.getResult()).thenReturn(generation);
            when(chatModel.call(any(Prompt.class))).thenReturn(response);

            // When/Then
            assertThatThrownBy(() -> llmService.generate("test prompt"))
                    .isInstanceOf(LlmException.class)
                    .hasMessageContaining("Empty response");
        }

        @Test
        @DisplayName("should throw LlmException when content is null")
        void shouldThrowLlmExceptionWhenContentIsNull() {
            // Given
            AssistantMessage message = mock(AssistantMessage.class);
            when(message.getContent()).thenReturn(null);
            Generation generation = mock(Generation.class);
            when(generation.getOutput()).thenReturn(message);
            ChatResponse response = mock(ChatResponse.class);
            when(response.getResult()).thenReturn(generation);
            when(chatModel.call(any(Prompt.class))).thenReturn(response);

            // When/Then
            assertThatThrownBy(() -> llmService.generate("test prompt"))
                    .isInstanceOf(LlmException.class)
                    .hasMessageContaining("Null content");
        }

        @Test
        @DisplayName("should wrap exceptions in LlmException")
        void shouldWrapExceptionsInLlmException() {
            // Given
            when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("Connection failed"));

            // When/Then
            assertThatThrownBy(() -> llmService.generate("test prompt"))
                    .isInstanceOf(LlmException.class)
                    .hasMessageContaining("Failed to generate response")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("generate(systemPrompt, userPrompt)")
    class GenerateWithSystemAndUserPrompt {

        @Test
        @DisplayName("should include both system and user messages")
        void shouldIncludeBothSystemAndUserMessages() {
            // Given
            String systemPrompt = "You are a language tutor";
            String userPrompt = "Translate 'Hello'";
            mockChatResponse("Bonjour");

            // When
            String result = llmService.generate(systemPrompt, userPrompt);

            // Then
            assertThat(result).isEqualTo("Bonjour");
            verify(chatModel).call(promptCaptor.capture());
            
            Prompt capturedPrompt = promptCaptor.getValue();
            assertThat(capturedPrompt.getInstructions()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("generate(template, variables)")
    class GenerateWithTemplate {

        @Test
        @DisplayName("should render template and generate response")
        void shouldRenderTemplateAndGenerateResponse() {
            // Given
            PromptTemplate template = PromptTemplate.of("Translate '{word}' to {language}");
            Map<String, Object> variables = Map.of("word", "Hello", "language", "French");
            mockChatResponse("Bonjour");

            // When
            String result = llmService.generate(template, variables);

            // Then
            assertThat(result).isEqualTo("Bonjour");
            verify(chatModel).call(promptCaptor.capture());
            assertThat(promptCaptor.getValue().getContents()).contains("Translate 'Hello' to French");
        }
    }

    @Nested
    @DisplayName("generateStream(userPrompt)")
    class GenerateStreamWithUserPrompt {

        @Test
        @DisplayName("should stream from streaming model")
        void shouldStreamFromStreamingModelWithUserPrompt() {
            // Given - use a mock that implements both interfaces
            StreamingChatModel streamingModel = mock(StreamingChatModel.class, withSettings().extraInterfaces(ChatModel.class));
            DefaultLlmService streamingService = new DefaultLlmService((ChatModel) streamingModel);

            ChatResponse chunk1 = createStreamingChunk("Hello ");
            ChatResponse chunk2 = createStreamingChunk("World");
            when(streamingModel.stream(any(Prompt.class))).thenReturn(Flux.just(chunk1, chunk2));

            // When
            Flux<String> result = streamingService.generateStream("test prompt");

            // Then
            StepVerifier.create(result)
                    .expectNext("Hello ")
                    .expectNext("World")
                    .verifyComplete();
        }


        @Test
        @DisplayName("should filter empty content from stream")
        void shouldFilterEmptyContentFromStream() {
            // Given
            StreamingChatModel streamingModel = mock(StreamingChatModel.class, withSettings().extraInterfaces(ChatModel.class));
            DefaultLlmService streamingService = new DefaultLlmService((ChatModel) streamingModel);

            ChatResponse chunk1 = createStreamingChunk("Hello");
            ChatResponse emptyChunk = createStreamingChunk("");
            ChatResponse chunk2 = createStreamingChunk("World");
            when(streamingModel.stream(any(Prompt.class))).thenReturn(Flux.just(chunk1, emptyChunk, chunk2));

            // When
            Flux<String> result = streamingService.generateStream("test prompt");

            // Then
            StepVerifier.create(result)
                    .expectNext("Hello")
                    .expectNext("World")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("generateStream(systemPrompt, userPrompt)")
    class GenerateStreamWithSystemAndUserPrompt {

        @Test
        @DisplayName("should stream with system and user prompts")
        void shouldStreamWithSystemAndUserPrompts() {
            // Given - use a mock that implements both interfaces
            StreamingChatModel streamingModel = mock(StreamingChatModel.class, withSettings().extraInterfaces(ChatModel.class));
            DefaultLlmService streamingService = new DefaultLlmService((ChatModel) streamingModel);

            ChatResponse chunk1 = createStreamingChunk("System ");
            ChatResponse chunk2 = createStreamingChunk("Response");
            when(streamingModel.stream(any(Prompt.class))).thenReturn(Flux.just(chunk1, chunk2));

            // When
            Flux<String> result = streamingService.generateStream("You are an assistant", "Hello");

            // Then
            StepVerifier.create(result)
                    .expectNext("System ")
                    .expectNext("Response")
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getChatModel")
    class GetChatModel {

        @Test
        @DisplayName("should return the injected chat model")
        void shouldReturnTheInjectedChatModel() {
            // When
            ChatModel result = llmService.getChatModel();

            // Then
            assertThat(result).isSameAs(chatModel);
        }
    }

    private void mockChatResponse(String content) {
        AssistantMessage message = new AssistantMessage(content);
        Generation generation = new Generation(message);
        ChatResponse response = new ChatResponse(java.util.List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
    }

    private ChatResponse createStreamingChunk(String content) {
        AssistantMessage message = new AssistantMessage(content);
        Generation generation = new Generation(message);
        return new ChatResponse(java.util.List.of(generation));
    }
}