package com.loja.online.checkout.dto;

import java.math.BigDecimal;

public record ResumoCheckoutResponseDTO(
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
