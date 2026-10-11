package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela,
        BigDecimal ajustePagamento) {
}
