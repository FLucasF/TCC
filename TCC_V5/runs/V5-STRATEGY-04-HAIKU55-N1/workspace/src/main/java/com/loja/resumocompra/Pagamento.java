package com.loja.resumocompra;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
