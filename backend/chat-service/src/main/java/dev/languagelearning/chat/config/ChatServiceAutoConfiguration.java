package dev.languagelearning.chat.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the chat service module.
 * <p>
 * This configuration enables component scanning for chat service beans.
 * JPA repositories and entity scanning are handled by the main application.
 */
@AutoConfiguration
@ComponentScan(basePackages = "dev.languagelearning.chat")
public class ChatServiceAutoConfiguration {
}
