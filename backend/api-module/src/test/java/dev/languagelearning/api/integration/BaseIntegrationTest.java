package dev.languagelearning.api.integration;

import dev.languagelearning.content.service.ContentGenerationService;
import dev.languagelearning.image.service.ImageService;
import dev.languagelearning.speech.service.SpeechService;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers with PostgreSQL.
 * <p>
 * This class provides:
 * <ul>
 *   <li>A PostgreSQL container that starts automatically</li>
 *   <li>Full Spring Boot context with auto-configured MockMvc</li>
 *   <li>Mock beans for external AI services (LLM, Speech, Image)</li>
 *   <li>Integration test profile configuration</li>
 * </ul>
 * <p>
 * Extend this class for all integration tests to ensure consistent setup.
 * <p>
 * Run integration tests with: {@code mvn verify -Dgroups=integration}
 *
 * @see org.testcontainers.containers.PostgreSQLContainer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("integration")
@Tag("integration")
public abstract class BaseIntegrationTest {

    /**
     * PostgreSQL container shared across all integration tests.
     * Uses PostgreSQL 16 Alpine image for consistency with production.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("language_learning_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    /**
     * MockMvc for making HTTP requests to the application.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * Mock for ContentGenerationService - AI content generation.
     * Override in individual tests to define specific behavior.
     */
    @MockBean
    protected ContentGenerationService contentGenerationService;

    /**
     * Mock for SpeechService - Text-to-Speech and Speech-to-Text.
     * Override in individual tests to define specific behavior.
     */
    @MockBean
    protected SpeechService speechService;

    /**
     * Mock for ImageService - Image generation.
     * Override in individual tests to define specific behavior.
     */
    @MockBean
    protected ImageService imageService;

    /**
     * Configures Spring datasource properties dynamically from Testcontainers.
     * <p>
     * This method is called before the Spring context is created and sets
     * the database connection properties to point to the running PostgreSQL container.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}