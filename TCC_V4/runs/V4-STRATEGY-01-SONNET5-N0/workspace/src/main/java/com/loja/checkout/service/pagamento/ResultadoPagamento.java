package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(
        BigDecimal ajuste,
        BigDecimal totalFinal,
        BigDecimal valorParcela
) {
}
