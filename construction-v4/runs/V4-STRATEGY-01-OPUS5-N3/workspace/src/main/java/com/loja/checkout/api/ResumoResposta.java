package com.loja.checkout.api;

import java.math.BigDecimal;

/** O resumo da compra, com todos os valores em dinheiro em 2 casas decimais. */
public record ResumoResposta(
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
        boolean brinde) {
}
