package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Resumo da compra mostrado ao cliente antes de confirmar o pedido. */
public record ResumoCompra(BigDecimal subtotalProdutos,
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
