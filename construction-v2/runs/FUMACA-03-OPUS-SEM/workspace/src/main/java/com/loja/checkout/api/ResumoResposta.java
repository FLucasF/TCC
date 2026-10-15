package com.loja.checkout.api;

import java.math.BigDecimal;

/** Resumo da compra mostrado ao cliente antes de confirmar o pedido. */
public record ResumoResposta(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela) {
}
