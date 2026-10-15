package com.loja.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ResumoResponse(
    BigDecimal subtotalProdutos,
    BigDecimal descontoCupom,
    BigDecimal frete,
    @JsonProperty("prazoEntregaDias")
    int prazoEntregaDias,
    BigDecimal seguro,
    BigDecimal ajustePagamento,
    BigDecimal totalFinal,
    int parcelas,
    BigDecimal valorParcela,
    BigDecimal creditoProximaCompra,
    boolean brinde
) {
}
