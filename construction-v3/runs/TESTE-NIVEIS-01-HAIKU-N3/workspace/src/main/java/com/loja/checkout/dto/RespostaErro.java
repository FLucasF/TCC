package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RespostaErro {
    @JsonProperty("erro")
    public String erro;

    public RespostaErro(String erro) {
        this.erro = erro;
    }

    public RespostaErro() {
    }
}
