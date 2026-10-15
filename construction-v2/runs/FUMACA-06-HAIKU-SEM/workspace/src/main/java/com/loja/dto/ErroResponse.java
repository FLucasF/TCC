package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErroResponse {
    @JsonProperty("erro")
    private String erro;

    public ErroResponse() {}

    public ErroResponse(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
