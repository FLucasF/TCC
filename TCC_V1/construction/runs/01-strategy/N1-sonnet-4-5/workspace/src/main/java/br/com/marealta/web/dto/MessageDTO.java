package br.com.marealta.web.dto;

import br.com.marealta.domain.Message;

public record MessageDTO(
    Long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public static MessageDTO from(Message message) {
        return new MessageDTO(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }
}
