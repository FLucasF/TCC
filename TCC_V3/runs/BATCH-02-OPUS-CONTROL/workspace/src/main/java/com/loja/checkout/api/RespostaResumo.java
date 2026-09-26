package com.loja.checkout.api;

import java.math.BigDecimal;

/** Resumo da compra mostrado antes de confirmar o pedido. */
public record RespostaResumo(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela) {
}
