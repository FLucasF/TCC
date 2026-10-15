package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

public record AjustePagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
