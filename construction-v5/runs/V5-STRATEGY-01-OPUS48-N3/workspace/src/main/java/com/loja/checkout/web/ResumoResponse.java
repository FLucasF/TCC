package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * O resumo da compra devolvido ao site. A ordem dos campos é a mesma da tabela
 * do enunciado.
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
