package com.loja.checkout.api.dto;

import java.math.BigDecimal;

/**
 * Resumo da compra devolvido ao site. Os campos ficam nesta ordem de proposito,
 * que e a ordem mostrada no resumo para o cliente.
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
