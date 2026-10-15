package com.loja.pedidos.pagamento;

import java.math.BigDecimal;

public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
