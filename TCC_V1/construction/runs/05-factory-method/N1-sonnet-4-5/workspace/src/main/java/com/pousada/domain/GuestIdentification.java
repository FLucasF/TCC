package com.pousada.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
    @JsonSubTypes.Type(CpfIdentification.class),
    @JsonSubTypes.Type(CnpjIdentification.class),
    @JsonSubTypes.Type(PassportIdentification.class)
})
public sealed interface GuestIdentification permits CpfIdentification, CnpjIdentification, PassportIdentification {
    String getGuestName();
    String formatForDocument();
    String generateDocument(Document.DocumentData data);
}
