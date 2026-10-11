package com.loja.resumo.calculo;

import java.math.BigDecimal;

record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
