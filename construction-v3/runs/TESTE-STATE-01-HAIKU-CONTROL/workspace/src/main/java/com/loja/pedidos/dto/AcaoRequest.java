package com.loja.pedidos.dto;

import com.google.gson.annotations.SerializedName;

public class AcaoRequest {
    @SerializedName("acao")
    private String acao;

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }
}
