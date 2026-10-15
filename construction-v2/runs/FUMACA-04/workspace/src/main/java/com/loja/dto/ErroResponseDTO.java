package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErroResponseDTO {

    @JsonProperty("erro")
    private String erro;

    public ErroResponseDTO() {
    }

    public ErroResponseDTO(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

}
