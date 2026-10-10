package com.loja.resumo.pagamento;

import java.math.BigDecimal;

public record AjustePagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
