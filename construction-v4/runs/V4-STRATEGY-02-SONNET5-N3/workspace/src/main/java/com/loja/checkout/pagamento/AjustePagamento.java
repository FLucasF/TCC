package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record AjustePagamento(BigDecimal totalFinal, BigDecimal valorParcela, BigDecimal ajuste) {
}
