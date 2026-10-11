package com.lojaroupas.dto;

import com.google.gson.annotations.SerializedName;

public class RespostaErro {
    @SerializedName("erro")
    public String erro;

    public RespostaErro() {}

    public RespostaErro(String erro) {
        this.erro = erro;
    }
}
