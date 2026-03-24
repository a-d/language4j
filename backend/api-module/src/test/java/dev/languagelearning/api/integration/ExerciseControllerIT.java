package dev.languagelearning.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.api.dto.*;
import dev.languagelearning.core.domain.ExerciseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link dev.languagelearning.api.controller.ExerciseController}.
 * <p>
 * Tests the exercise API endpoints with a real PostgreSQL database.
 * AI-powered content generation is mocked.
 */
class ExerciseControllerIT extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure user exists before each test
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("POST /api/v1/exercises/text-completion")
    class TextCompletionExercises {

        @Test
        @DisplayName("should generate text completion exercises")
        void shouldGenerateTextCompletionExercises() throws Exception {
            String mockResponse = """
                    {
                        "exercises": [
                            {
                                "sentence": "Je ___ français.",
                                "answer": "parle",
                                "hint": "to speak"
                            }
                        ]
                    }
                    """;

            when(contentGenerationService.generateTextCompletionExercises(eq("greetings"), eq(3)))
                    .thenReturn(mockResponse);

            GenerateExerciseRequest request = new GenerateExerciseRequest("greetings", 3);

            mockMvc.perform(post("/api/v1/exercises/text-completion")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("text-completion"));

            verify(contentGenerationService).generateTextCompletionExercises("greetings", 3);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/exercises/drag-drop")
    class DragDropExercises {

        @Test
        @DisplayName("should generate drag-drop exercises")
        void shouldGenerateDragDropExercises() throws Exception {
            String mockResponse = """
                    {
                        "exercises": [
                            {
                                "shuffledWords": ["parle", "français", "Je"],
                                "correctOrder": ["Je", "parle", "français"],
                                "translation": "I speak French"
                            }
                        ]
                    }
                    """;

            when(contentGenerationService.generateDragDropExercises(eq("vocabulary"), eq(5)))
                    .thenReturn(mockResponse);

            GenerateExerciseRequest request = new GenerateExerciseRequest("vocabulary", 5);

            mockMvc.perform(post("/api/v1/exercises/drag-drop")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("drag-drop"));

            verify(contentGenerationService).generateDragDropExercises("vocabulary", 5);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/exercises/translation")
    class TranslationExercises {

        @Test
        @DisplayName("should generate translation exercises")
        void shouldGenerateTranslationExercises() throws Exception {
            String mockResponse = """
                    {
                        "exercises": [
                            {
                                "source": "Guten Morgen",
                                "target": "Bonjour",
                                "vocabulary": ["morgen", "gut"]
                            }
                        ]
                    }
                    """;

            when(contentGenerationService.generateTranslationExercises(eq("morning"), eq(3)))
                    .thenReturn(mockResponse);

            GenerateExerciseRequest request = new GenerateExerciseRequest("morning", 3);

            mockMvc.perform(post("/api/v1/exercises/translation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("translation"));

            verify(contentGenerationService).generateTranslationExercises("morning", 3);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/exercises/evaluate")
    class EvaluateResponse {

        @Test
        @DisplayName("should evaluate user response")
        void shouldEvaluateUserResponse() throws Exception {
            String mockResponse = """
                    {
                        "correct": true,
                        "feedback": "Great job!",
                        "score": 100
                    }
                    """;

            when(contentGenerationService.evaluateResponse(
                    eq("Fill in: Je ___ français"),
                    eq("parle"),
                    eq("parle")))
                    .thenReturn(mockResponse);

            EvaluateResponseRequest request = new EvaluateResponseRequest(
                    "Fill in: Je ___ français",
                    "parle",
                    "parle"
            );

            mockMvc.perform(post("/api/v1/exercises/evaluate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("evaluation"));

            verify(contentGenerationService).evaluateResponse(
                    "Fill in: Je ___ français",
                    "parle",
                    "parle"
            );
        }
    }

    @Nested
    @DisplayName("Exercise Results API")
    class ExerciseResults {

        @Test
        @DisplayName("should save exercise result")
        void shouldSaveExerciseResult() throws Exception {
            SaveExerciseResultRequest request = new SaveExerciseResultRequest(
                    "TEXT_COMPLETION",
                    "greetings-lesson-1",
                    80,
                    4,
                    5,
                    120L,
                    "[\"parle\", \"habite\", \"suis\", \"aime\"]",
                    "[\"parle\", \"habite\", \"suis\", \"aime\", \"travaille\"]",
                    "Good job! You got 4 out of 5 correct."
            );

            mockMvc.perform(post("/api/v1/exercises/results")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.exerciseType").value("TEXT_COMPLETION"))
                    .andExpect(jsonPath("$.exerciseReference").value("greetings-lesson-1"))
                    .andExpect(jsonPath("$.score").value(80))
                    .andExpect(jsonPath("$.correctAnswers").value(4))
                    .andExpect(jsonPath("$.totalQuestions").value(5))
                    .andExpect(jsonPath("$.timeSpentSeconds").value(120))
                    .andExpect(jsonPath("$.passed").value(true));
        }

        @Test
        @DisplayName("should save exercise result with minimum fields")
        void shouldSaveExerciseResultWithMinimumFields() throws Exception {
            SaveExerciseResultRequest request = new SaveExerciseResultRequest(
                    "DRAG_DROP",
                    null,
                    60,
                    3,
                    5,
                    90L,
                    null,
                    null,
                    null
            );

            mockMvc.perform(post("/api/v1/exercises/results")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exerciseType").value("DRAG_DROP"))
                    .andExpect(jsonPath("$.score").value(60))
                    .andExpect(jsonPath("$.passed").value(false));
        }

        @Test
        @DisplayName("should validate exercise type")
        void shouldValidateExerciseType() throws Exception {
            String invalidRequest = """
                    {
                        "exerciseType": "INVALID_TYPE",
                        "score": 80,
                        "correctAnswers": 4,
                        "totalQuestions": 5,
                        "timeSpentSeconds": 120
                    }
                    """;

            mockMvc.perform(post("/api/v1/exercises/results")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should validate score range")
        void shouldValidateScoreRange() throws Exception {
            SaveExerciseResultRequest request = new SaveExerciseResultRequest(
                    "TEXT_COMPLETION",
                    null,
                    150, // Invalid: > 100
                    5,
                    5,
                    60L,
                    null,
                    null,
                    null
            );

            mockMvc.perform(post("/api/v1/exercises/results")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should retrieve exercise history")
        void shouldRetrieveExerciseHistory() throws Exception {
            // First, save some results
            saveTestResult("TEXT_COMPLETION", 80);
            saveTestResult("DRAG_DROP", 70);
            saveTestResult("TRANSLATION", 90);

            mockMvc.perform(get("/api/v1/exercises/results"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)));
        }

        @Test
        @DisplayName("should filter history by exercise type")
        void shouldFilterHistoryByExerciseType() throws Exception {
            // Save results of different types
            saveTestResult("TEXT_COMPLETION", 80);
            saveTestResult("TEXT_COMPLETION", 85);
            saveTestResult("DRAG_DROP", 70);

            mockMvc.perform(get("/api/v1/exercises/results")
                            .param("type", "TEXT_COMPLETION"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[*].exerciseType", everyItem(is("TEXT_COMPLETION"))));
        }

        @Test
        @DisplayName("should paginate results")
        void shouldPaginateResults() throws Exception {
            // Save multiple results
            for (int i = 0; i < 5; i++) {
                saveTestResult("TEXT_COMPLETION", 70 + i);
            }

            mockMvc.perform(get("/api/v1/exercises/results")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(5)));
        }

        @Test
        @DisplayName("should get recent results")
        void shouldGetRecentResults() throws Exception {
            saveTestResult("TEXT_COMPLETION", 80);

            mockMvc.perform(get("/api/v1/exercises/results/recent")
                            .param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/exercises/statistics")
    class ExerciseStatistics {

        @Test
        @DisplayName("should return exercise statistics")
        void shouldReturnExerciseStatistics() throws Exception {
            // Save some results first
            saveTestResult("TEXT_COMPLETION", 80);
            saveTestResult("DRAG_DROP", 70);
            saveTestResult("TRANSLATION", 90);

            mockMvc.perform(get("/api/v1/exercises/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalExercises").value(greaterThanOrEqualTo(3)))
                    .andExpect(jsonPath("$.averageScore").exists())
                    .andExpect(jsonPath("$.totalTimeSeconds").value(greaterThanOrEqualTo(0)))
                    .andExpect(jsonPath("$.passRate").exists())
                    .andExpect(jsonPath("$.countsByType").isMap());
        }

        @Test
        @DisplayName("should track exercises completed today")
        void shouldTrackExercisesCompletedToday() throws Exception {
            // Save results today
            saveTestResult("TEXT_COMPLETION", 85);
            saveTestResult("TEXT_COMPLETION", 90);

            mockMvc.perform(get("/api/v1/exercises/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exercisesToday").value(greaterThanOrEqualTo(2)));
        }
    }

    @Nested
    @DisplayName("All Exercise Types")
    class AllExerciseTypes {

        @Test
        @DisplayName("should accept all valid exercise types when saving results")
        void shouldAcceptAllValidExerciseTypes() throws Exception {
            for (ExerciseType type : ExerciseType.values()) {
                SaveExerciseResultRequest request = new SaveExerciseResultRequest(
                        type.name(),
                        "test-reference",
                        75,
                        3,
                        4,
                        60L,
                        null,
                        null,
                        null
                );

                mockMvc.perform(post("/api/v1/exercises/results")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.exerciseType").value(type.name()));
            }
        }
    }

    @Nested
    @DisplayName("Listening and Speaking Exercises")
    class ListeningAndSpeakingExercises {

        @Test
        @DisplayName("should generate listening exercises")
        void shouldGenerateListeningExercises() throws Exception {
            String mockResponse = """
                    {
                        "exercises": [
                            {
                                "text": "Bonjour, comment allez-vous?",
                                "translation": "Hello, how are you?",
                                "hint": "A common greeting"
                            }
                        ]
                    }
                    """;

            when(contentGenerationService.generateListeningExercises(eq("greetings"), eq(3)))
                    .thenReturn(mockResponse);

            GenerateExerciseRequest request = new GenerateExerciseRequest("greetings", 3);

            mockMvc.perform(post("/api/v1/exercises/listening")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("listening"));

            verify(contentGenerationService).generateListeningExercises("greetings", 3);
        }

        @Test
        @DisplayName("should generate speaking exercises")
        void shouldGenerateSpeakingExercises() throws Exception {
            String mockResponse = """
                    {
                        "exercises": [
                            {
                                "text": "Je m'appelle...",
                                "translation": "My name is...",
                                "pronunciationTips": "Focus on the nasal 'en' sound",
                                "commonMistakes": ["Incorrect liaison"]
                            }
                        ]
                    }
                    """;

            when(contentGenerationService.generateSpeakingExercises(eq("introductions"), eq(3)))
                    .thenReturn(mockResponse);

            GenerateExerciseRequest request = new GenerateExerciseRequest("introductions", 3);

            mockMvc.perform(post("/api/v1/exercises/speaking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("speaking"));

            verify(contentGenerationService).generateSpeakingExercises("introductions", 3);
        }

        @Test
        @DisplayName("should evaluate pronunciation")
        void shouldEvaluatePronunciation() throws Exception {
            String mockResponse = """
                    {
                        "accuracy": 85,
                        "feedback": "Good pronunciation overall",
                        "mistakes": ["Minor liaison error"],
                        "tips": ["Practice the nasal sounds"]
                    }
                    """;

            when(contentGenerationService.evaluatePronunciation(
                    eq("Bonjour, comment allez-vous?"),
                    eq("Bonjour, comment allez vous?")))
                    .thenReturn(mockResponse);

            EvaluatePronunciationRequest request = new EvaluatePronunciationRequest(
                    "Bonjour, comment allez-vous?",
                    "Bonjour, comment allez vous?"
            );

            mockMvc.perform(post("/api/v1/exercises/evaluate-pronunciation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("pronunciation-evaluation"));

            verify(contentGenerationService).evaluatePronunciation(
                    "Bonjour, comment allez-vous?",
                    "Bonjour, comment allez vous?"
            );
        }
    }

    /**
     * Helper method to save a test exercise result.
     */
    private void saveTestResult(String exerciseType, int score) throws Exception {
        SaveExerciseResultRequest request = new SaveExerciseResultRequest(
                exerciseType,
                "test-reference",
                score,
                4,
                5,
                60L,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/exercises/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}