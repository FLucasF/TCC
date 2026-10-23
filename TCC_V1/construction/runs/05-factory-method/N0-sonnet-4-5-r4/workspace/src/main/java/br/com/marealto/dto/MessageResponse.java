package br.com.marealto.dto;

import br.com.marealto.model.Message;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageResponse(
        Long id,
        String channel,
        @JsonProperty("to") String to,
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
