package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record Cobranca(BigDecimal valorFinal, BigDecimal valorParcela) {
}
