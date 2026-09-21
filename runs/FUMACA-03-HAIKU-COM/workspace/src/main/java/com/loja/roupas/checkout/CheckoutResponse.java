package com.loja.roupas.checkout;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CheckoutResponse(
    @JsonProperty("subtotalProdutos")
    BigDecimal subtotalProdutos,
    @JsonProperty("descontoCupom")
    BigDecimal descontoCupom,
    @JsonProperty("frete")
    BigDecimal frete,
    @JsonProperty("prazoEntregaDias")
    Integer prazoEntregaDias,
    @JsonProperty("ajustePagamento")
    BigDecimal ajustePagamento,
    @JsonProperty("totalFinal")
    BigDecimal totalFinal,
    @JsonProperty("parcelas")
    Integer parcelas,
    @JsonProperty("valorParcela")
    BigDecimal valorParcela
) {}
