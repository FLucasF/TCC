package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
