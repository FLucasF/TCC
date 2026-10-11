package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckoutError {
    @JsonProperty("erro")
    private String erro;

    public CheckoutError() {}

    public CheckoutError(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
