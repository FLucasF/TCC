package com.loja.checkout.calculo;

import java.math.BigDecimal;

record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela, int parcelas) {
}
