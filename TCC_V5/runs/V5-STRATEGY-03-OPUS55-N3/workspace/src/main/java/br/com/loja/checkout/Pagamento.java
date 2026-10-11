package br.com.loja.checkout;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
