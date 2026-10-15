package com.loja.checkout.api;

import java.math.BigDecimal;

/** O resumo mostrado ao cliente antes de confirmar a compra. */
public record ResumoResponse(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela) {
}
