package com.loja.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponseDTO {
    @JsonProperty("erro")
    public String erro;

    public ErrorResponseDTO() {}

    public ErrorResponseDTO(String erro) {
        this.erro = erro;
    }

    public String getErro() {
        return erro;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }
}
