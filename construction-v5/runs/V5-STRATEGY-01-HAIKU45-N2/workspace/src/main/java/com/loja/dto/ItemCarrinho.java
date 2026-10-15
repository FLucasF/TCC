package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemCarrinho {
  @JsonProperty("nome")
  public String nome;

  @JsonProperty("precoUnitario")
  public Double precoUnitario;

  @JsonProperty("quantidade")
  public Integer quantidade;

  @JsonProperty("pesoKg")
  public Double pesoKg;
}
