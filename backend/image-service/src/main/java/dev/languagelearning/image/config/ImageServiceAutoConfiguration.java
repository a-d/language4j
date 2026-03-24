package dev.languagelearning.image.config;

import dev.languagelearning.config.AiProviderConfig;
import dev.languagelearning.image.service.ImageService;
import dev.languagelearning.image.service.impl.ImageServiceImpl;
import org.springframework.ai.image.ImageModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Image Service.
 * <p>
 * This configuration ensures that {@link ImageService} is only created when
 * an {@link ImageModel} bean is available (e.g., when OpenAI DALL-E is configured).
 */
@AutoConfiguration
@ConditionalOnBean(ImageModel.class)
public class ImageServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ImageService.class)
    public ImageService imageService(ImageModel imageModel, AiProviderConfig aiProviderConfig) {
        return new ImageServiceImpl(imageModel, aiProviderConfig);
    }
}