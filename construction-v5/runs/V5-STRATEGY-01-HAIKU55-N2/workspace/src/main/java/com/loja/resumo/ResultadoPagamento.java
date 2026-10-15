package com.loja.resumo;

import java.math.BigDecimal;

record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
