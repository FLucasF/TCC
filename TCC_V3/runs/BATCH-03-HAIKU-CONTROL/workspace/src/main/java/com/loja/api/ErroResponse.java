package com.loja.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErroResponse {
    @JsonProperty("erro")
    private String erro;

    public ErroResponse(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }
}
