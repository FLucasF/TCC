package com.loja.pedidos;

public class ErrorResponse {
    private String erro;

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
