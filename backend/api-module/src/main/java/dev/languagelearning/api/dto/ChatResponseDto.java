package dev.languagelearning.api.dto;

import java.util.List;

/**
 * Response containing a chat message and suggestions.
 */
public record ChatResponseDto(
        ChatMessageDto message,
        List<String> suggestions
) {}