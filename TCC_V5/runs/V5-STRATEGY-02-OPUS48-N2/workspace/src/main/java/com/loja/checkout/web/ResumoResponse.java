package com.loja.checkout.web;

import java.math.BigDecimal;

/** O resumo da compra, como o site espera de volta. Valores em dinheiro com 2 casas. */
public record ResumoResponse(
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
