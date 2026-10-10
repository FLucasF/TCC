package com.loja.checkout.api;

import java.math.BigDecimal;

/**
 * O resumo da compra devolvido ao site. Todos os valores em dinheiro vêm com
 * 2 casas decimais.
 */
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
