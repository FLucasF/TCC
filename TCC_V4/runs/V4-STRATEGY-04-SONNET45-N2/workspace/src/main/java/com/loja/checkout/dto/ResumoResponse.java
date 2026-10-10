package com.loja.checkout.dto;

import java.math.BigDecimal;

public record ResumoResponse(
    BigDecimal subtotalProdutos,
    BigDecimal descontoCupom,
    BigDecimal frete,
    Integer prazoEntregaDias,
    BigDecimal seguro,
    BigDecimal ajustePagamento,
    BigDecimal totalFinal,
    Integer parcelas,
    BigDecimal valorParcela,
    BigDecimal creditoProximaCompra,
    Boolean brinde
) {}
