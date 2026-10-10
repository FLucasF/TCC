package com.loja.checkout.service.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
