package com.loja.checkout.dto;

import java.math.BigDecimal;

/** Resumo da compra devolvido ao site quando o cálculo dá certo. */
public record CheckoutResponse(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
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
