package com.loja.checkout.aplicacao;

import java.math.BigDecimal;

/** O resumo que o site mostra antes de o cliente confirmar o pedido. */
public record ResumoDaCompra(
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
