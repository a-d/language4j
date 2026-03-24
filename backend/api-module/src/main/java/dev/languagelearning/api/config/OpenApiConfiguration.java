package dev.languagelearning.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for the Language Learning Platform API.
 * <p>
 * This class configures the OpenAPI metadata displayed in Swagger UI.
 * The full API specification is also available at {@code docs/openapi.yaml}.
 */
@Configuration
public class OpenApiConfiguration {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI languageLearningOpenApi() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers());
    }

    private Info apiInfo() {
        return new Info()
                .title("Language Learning Platform API")
                .description("""
                        AI-powered language learning platform REST API.
                        
                        This API provides endpoints for:
                        - **User management** and profile configuration
                        - **Learning goals** (daily, weekly, monthly, yearly)
                        - **AI-generated content** (lessons, vocabulary, flashcards, scenarios)
                        - **Interactive exercises** (text completion, drag-drop, translation)
                        
                        ## Authentication
                        Currently, the API operates in single-user mode. Future versions will support
                        multi-user authentication via JWT tokens.
                        
                        ## Content Generation
                        Content generation endpoints use AI (LLM) to create personalized learning materials.
                        Response times may vary based on the AI provider configuration.
                        
                        ## Static Specification
                        The full OpenAPI specification is available in `docs/openapi.yaml`.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Language Learning Platform")
                        .url("https://github.com/a-d/language4j"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local development server"),
                new Server()
                        .url("http://localhost:8080")
                        .description("Docker development server")
        );
    }
}