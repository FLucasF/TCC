package br.com.pousadamarealta.api.dto;

import br.com.pousadamarealta.domain.Message;

public record MessageDto(Long id, String channel, String to, String subject, String body) {
    public static MessageDto from(Message message) {
        return new MessageDto(
            message.id(),
            message.channel().name(),
            message.to(),
            message.subject(),
            message.body()
        );
    }
}
