package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Resumo da compra que o servico devolve. Os campos saem na mesma ordem do
 * enunciado e os valores em dinheiro vao com 2 casas decimais.
 */
public record ResumoCompraResponse(
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
