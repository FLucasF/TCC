package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
