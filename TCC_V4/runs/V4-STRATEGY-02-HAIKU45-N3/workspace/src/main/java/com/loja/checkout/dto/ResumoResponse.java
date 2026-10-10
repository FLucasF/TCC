package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumoResponse(
    @JsonProperty("subtotalProdutos") double subtotalProdutos,
    @JsonProperty("descontoCupom") double descontoCupom,
    @JsonProperty("frete") double frete,
    @JsonProperty("prazoEntregaDias") int prazoEntregaDias,
    @JsonProperty("seguro") double seguro,
    @JsonProperty("ajustePagamento") double ajustePagamento,
    @JsonProperty("totalFinal") double totalFinal,
    @JsonProperty("parcelas") int parcelas,
    @JsonProperty("valorParcela") double valorParcela,
    @JsonProperty("creditoProximaCompra") double creditoProximaCompra,
    @JsonProperty("brinde") boolean brinde
) {}
