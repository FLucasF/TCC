package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Resumo(BigDecimal subtotalProdutos,
                     BigDecimal descontoCupom,
                     BigDecimal frete,
                     int prazoEntregaDias,
                     BigDecimal ajustePagamento,
                     BigDecimal totalFinal,
                     int parcelas,
                     BigDecimal valorParcela) {
}
