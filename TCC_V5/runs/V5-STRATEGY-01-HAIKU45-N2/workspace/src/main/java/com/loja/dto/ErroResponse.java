package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErroResponse {
  @JsonProperty("erro")
  public String erro;

  public ErroResponse(String erro) {
    this.erro = erro;
  }
}
