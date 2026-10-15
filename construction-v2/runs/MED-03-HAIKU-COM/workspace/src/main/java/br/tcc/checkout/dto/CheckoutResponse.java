package br.tcc.checkout.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
    BigDecimal subtotalProdutos,
    BigDecimal descontoCupom,
    BigDecimal frete,
    Integer prazoEntregaDias,
    BigDecimal ajustePagamento,
    BigDecimal totalFinal,
    Integer parcelas,
    BigDecimal valorParcela
) {}
