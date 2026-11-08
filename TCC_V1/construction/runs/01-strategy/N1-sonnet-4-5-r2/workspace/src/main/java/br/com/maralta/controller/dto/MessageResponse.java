package br.com.maralta.controller.dto;

import br.com.maralta.domain.Message;

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
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
