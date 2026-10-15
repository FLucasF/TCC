package com.loja.checkout.dto;

public class ErrorResponse {

    private final String erro;

    public ErrorResponse(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }
}
