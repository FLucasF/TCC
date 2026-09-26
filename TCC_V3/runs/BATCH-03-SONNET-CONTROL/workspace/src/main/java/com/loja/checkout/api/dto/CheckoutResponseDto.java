package com.loja.checkout.api.dto;

import java.math.BigDecimal;

public record CheckoutResponseDto(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela
) {
}
