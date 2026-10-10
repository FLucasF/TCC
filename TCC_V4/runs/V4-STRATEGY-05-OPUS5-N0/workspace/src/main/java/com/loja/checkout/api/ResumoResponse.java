package com.loja.checkout.api;

import java.math.BigDecimal;

/** Resumo da compra devolvido para o site. */
public record ResumoResponse(BigDecimal subtotalProdutos,
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
