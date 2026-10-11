package com.loja.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CheckoutRequest {
  @JsonProperty("itens")
  public List<ItemCarrinho> itens;

  @JsonProperty("modalidadeEntrega")
  public String modalidadeEntrega;

  @JsonProperty("cupom")
  public String cupom;

  @JsonProperty("formaPagamento")
  public String formaPagamento;

  @JsonProperty("parcelas")
  public Integer parcelas;

  @JsonProperty("nivelClube")
  public String nivelClube;

  @JsonProperty("regiao")
  public String regiao;
}
