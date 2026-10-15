package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
  @JsonProperty("erro")
  public String erro;

  public ErrorResponse(String erro) {
    this.erro = erro;
  }
}
