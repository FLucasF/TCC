package br.com.marealta.api.dto;

import br.com.marealta.domain.Message;

public record MessageResponse(
    Long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
