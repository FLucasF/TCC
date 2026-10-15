package com.loja.resumo.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
