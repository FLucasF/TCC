package br.com.pousada.marealta.api.dto;

import br.com.pousada.marealta.model.Message;

public record MessageResponse(
        long id,
        String channel,
        String to,
        String subject,
        String body
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChannel().toString(),
                message.getTo(),
                message.getSubject(),
                message.getBody()
        );
    }
}
