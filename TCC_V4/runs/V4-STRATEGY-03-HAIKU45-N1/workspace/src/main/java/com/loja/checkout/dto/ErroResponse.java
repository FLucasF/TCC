package com.loja.checkout.dto;

public class ErroResponse {
    private String erro;

    public ErroResponse() {
    }

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
