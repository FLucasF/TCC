package com.loja.checkout.resumo;

import java.math.BigDecimal;

/** O resumo que o site mostra antes do cliente confirmar o pedido. */
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
