package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
    @JsonProperty
    private String erro;

    public ErrorResponse(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }
}
