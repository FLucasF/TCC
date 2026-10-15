package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class CheckoutResponse {
  @JsonProperty("subtotalProdutos")
  public BigDecimal subtotalProdutos;

  @JsonProperty("descontoCupom")
  public BigDecimal descontoCupom;

  @JsonProperty("frete")
  public BigDecimal frete;

  @JsonProperty("prazoEntregaDias")
  public int prazoEntregaDias;

  @JsonProperty("seguro")
  public BigDecimal seguro;

  @JsonProperty("ajustePagamento")
  public BigDecimal ajustePagamento;

  @JsonProperty("totalFinal")
  public BigDecimal totalFinal;

  @JsonProperty("parcelas")
  public int parcelas;

  @JsonProperty("valorParcela")
  public BigDecimal valorParcela;

  @JsonProperty("creditoProximaCompra")
  public BigDecimal creditoProximaCompra;

  @JsonProperty("brinde")
  public boolean brinde;
}
