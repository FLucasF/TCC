package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.model.Email;

public record EmailResponse(
    @JsonProperty("id") long id,
    @JsonProperty("channel") String channel,
    @JsonProperty("to") String to,
    @JsonProperty("subject") String subject,
    @JsonProperty("body") String body
) {
    public static EmailResponse from(Email email) {
        return new EmailResponse(
            email.id(),
            email.channel(),
            email.to(),
            email.subject(),
            email.body()
        );
    }
}
