package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Resumo da compra devolvido quando o cálculo dá certo. Todos os valores em
 * dinheiro já vêm com 2 casas decimais.
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
