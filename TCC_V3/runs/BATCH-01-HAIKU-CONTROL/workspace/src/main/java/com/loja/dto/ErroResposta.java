package com.loja.dto;

public class ErroResposta {
    private String erro;

    public ErroResposta(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
