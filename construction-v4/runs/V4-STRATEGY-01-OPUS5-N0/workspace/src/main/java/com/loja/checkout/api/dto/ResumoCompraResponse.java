package com.loja.checkout.api.dto;

import java.math.BigDecimal;

/** O resumo que o site mostra ao cliente antes de confirmar o pedido. */
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
