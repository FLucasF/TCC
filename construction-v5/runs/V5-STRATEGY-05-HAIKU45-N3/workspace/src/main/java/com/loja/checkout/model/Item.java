package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Item {
  @JsonProperty("nome")
  public String nome;

  @JsonProperty("precoUnitario")
  public double precoUnitario;

  @JsonProperty("quantidade")
  public int quantidade;

  @JsonProperty("pesoKg")
  public double pesoKg;
}
