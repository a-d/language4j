package dev.languagelearning.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration class that enables all configuration properties.
 * <p>
 * This class is automatically loaded by Spring Boot when the config-module
 * is on the classpath. It registers all configuration property classes.
 */
@Configuration
@EnableConfigurationProperties({
        LanguageConfig.class,
        AiProviderConfig.class,
        StorageConfig.class
})
public class ConfigAutoConfiguration {
    // Configuration properties are automatically bound
}