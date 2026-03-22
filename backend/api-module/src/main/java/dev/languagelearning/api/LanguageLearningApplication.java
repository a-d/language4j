package dev.languagelearning.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class for the Language Learning Platform.
 * <p>
 * This application provides an AI-powered language learning experience with:
 * <ul>
 *   <li>Personalized learning plan generation</li>
 *   <li>Interactive exercises (text completion, drag-drop, speaking)</li>
 *   <li>Progress tracking and skill assessment</li>
 *   <li>Multi-provider AI support (OpenAI, Anthropic, Ollama)</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "dev.languagelearning")
@EntityScan(basePackages = "dev.languagelearning.core.domain")
@EnableJpaRepositories(basePackages = "dev.languagelearning.core.repository")
public class LanguageLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(LanguageLearningApplication.class, args);
    }
}