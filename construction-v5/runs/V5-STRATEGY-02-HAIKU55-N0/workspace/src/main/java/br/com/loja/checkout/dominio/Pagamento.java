package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Pagamento(BigDecimal ajuste, BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
