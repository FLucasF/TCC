package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RespostaErro {
    @JsonProperty("erro")
    private String erro;

    public RespostaErro(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }
}
