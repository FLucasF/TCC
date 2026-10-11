package com.loja.checkout;

import java.math.BigDecimal;

record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
