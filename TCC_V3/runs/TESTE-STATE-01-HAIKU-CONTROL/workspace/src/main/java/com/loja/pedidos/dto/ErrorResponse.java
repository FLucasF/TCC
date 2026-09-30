package com.loja.pedidos.dto;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {
    @SerializedName("erro")
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
