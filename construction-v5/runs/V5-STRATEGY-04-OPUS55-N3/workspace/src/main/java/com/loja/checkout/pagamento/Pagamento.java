package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
