package dev.languagelearning.speech.config;

import dev.languagelearning.config.AiProviderConfig;
import dev.languagelearning.speech.service.SpeechService;
import dev.languagelearning.speech.service.impl.SpeechServiceImpl;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Speech Service.
 * <p>
 * This configuration ensures that {@link SpeechService} is only created when
 * the required audio models are available (e.g., when OpenAI TTS is configured).
 * <p>
 * The transcription model is optional - if not configured, speech-to-text
 * functionality will throw an exception when called.
 */
@Configuration
@ConditionalOnBean(OpenAiAudioSpeechModel.class)
public class SpeechServiceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SpeechService.class)
    public SpeechService speechService(
            OpenAiAudioSpeechModel speechModel,
            ObjectProvider<OpenAiAudioTranscriptionModel> transcriptionModelProvider,
            AiProviderConfig aiProviderConfig) {
        return new SpeechServiceImpl(speechModel, transcriptionModelProvider.getIfAvailable(), aiProviderConfig);
    }
}
