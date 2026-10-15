package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErroResposta {
    @JsonProperty("erro")
    private String erro;

    public ErroResposta(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }
}
