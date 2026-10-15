package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Resposta de sucesso, exatamente com os nomes que o site espera.
 */
public record ResumoCompra(
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
