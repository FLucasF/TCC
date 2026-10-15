package br.tcc.checkout.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumoCheckoutResponse(
    @JsonProperty("subtotalProdutos")
    double subtotalProdutos,
    @JsonProperty("descontoCupom")
    double descontoCupom,
    double frete,
    @JsonProperty("prazoEntregaDias")
    int prazoEntregaDias,
    @JsonProperty("ajustePagamento")
    double ajustePagamento,
    @JsonProperty("totalFinal")
    double totalFinal,
    int parcelas,
    @JsonProperty("valorParcela")
    double valorParcela
) {}
