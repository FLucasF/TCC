package br.com.loja.checkout.resumo;

import java.math.BigDecimal;

/** Resumo mostrado ao cliente antes de confirmar a compra. */
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
