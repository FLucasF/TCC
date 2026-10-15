package com.loja.checkout.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CheckoutResponse {
  @JsonProperty("subtotalProdutos")
  public double subtotalProdutos;

  @JsonProperty("descontoCupom")
  public double descontoCupom;

  @JsonProperty("frete")
  public double frete;

  @JsonProperty("prazoEntregaDias")
  public int prazoEntregaDias;

  @JsonProperty("seguro")
  public double seguro;

  @JsonProperty("ajustePagamento")
  public double ajustePagamento;

  @JsonProperty("totalFinal")
  public double totalFinal;

  @JsonProperty("parcelas")
  public int parcelas;

  @JsonProperty("valorParcela")
  public double valorParcela;

  @JsonProperty("creditoProximaCompra")
  public double creditoProximaCompra;

  @JsonProperty("brinde")
  public boolean brinde;
}
