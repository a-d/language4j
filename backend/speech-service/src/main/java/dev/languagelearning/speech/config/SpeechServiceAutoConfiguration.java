package dev.languagelearning.speech.config;

import dev.languagelearning.config.AiProviderConfig;
import dev.languagelearning.speech.service.SpeechService;
import dev.languagelearning.speech.service.impl.SpeechServiceImpl;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Speech Service.
 * <p>
 * This configuration ensures that {@link SpeechService} is only created when
 * the required audio models are available (e.g., when OpenAI TTS is configured).
 */
@AutoConfiguration
@ConditionalOnBean(OpenAiAudioSpeechModel.class)
public class SpeechServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SpeechService.class)
    public SpeechService speechService(
            OpenAiAudioSpeechModel speechModel,
            OpenAiAudioTranscriptionModel transcriptionModel,
            AiProviderConfig aiProviderConfig) {
        return new SpeechServiceImpl(speechModel, transcriptionModel, aiProviderConfig);
    }
}