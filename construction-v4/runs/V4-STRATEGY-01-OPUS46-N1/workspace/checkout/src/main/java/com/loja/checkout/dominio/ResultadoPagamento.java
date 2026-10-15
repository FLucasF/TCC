package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record ResultadoPagamento(
        BigDecimal ajuste,
        BigDecimal totalFinal,
        BigDecimal valorParcela,
        int parcelas
) {
}
