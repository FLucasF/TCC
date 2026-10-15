package com.loja.checkout.model.dto;

public class ErrorResponse {
    private String erro;

    public ErrorResponse() {}

    public ErrorResponse(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
