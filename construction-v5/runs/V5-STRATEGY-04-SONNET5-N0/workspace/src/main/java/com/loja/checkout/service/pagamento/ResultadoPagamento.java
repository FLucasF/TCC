package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela
) {
}
