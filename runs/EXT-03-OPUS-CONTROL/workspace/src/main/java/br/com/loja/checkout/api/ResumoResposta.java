package br.com.loja.checkout.api;

import java.math.BigDecimal;

/** O resumo da compra mostrado antes de confirmar o pedido. */
public record ResumoResposta(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal imposto,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela,
        BigDecimal creditoProximaCompra,
        boolean brinde) {
}
