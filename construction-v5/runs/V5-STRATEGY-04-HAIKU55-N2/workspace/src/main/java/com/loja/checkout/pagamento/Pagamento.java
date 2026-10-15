package com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record Pagamento(BigDecimal valorFinal, BigDecimal valorParcela, int parcelas) {
}
