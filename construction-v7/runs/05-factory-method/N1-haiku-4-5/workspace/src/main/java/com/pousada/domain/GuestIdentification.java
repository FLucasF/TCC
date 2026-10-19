package com.pousada.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CpfIdentification.class, name = "cpf"),
    @JsonSubTypes.Type(value = CnpjIdentification.class, name = "cnpj"),
    @JsonSubTypes.Type(value = PassportIdentification.class, name = "passport")
})
public sealed interface GuestIdentification permits CpfIdentification, CnpjIdentification, PassportIdentification {
}
