package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErroResponse {
    @JsonProperty("erro")
    public String erro;

    public ErroResponse() {}

    public ErroResponse(String erro) {
        this.erro = erro;
    }
}
