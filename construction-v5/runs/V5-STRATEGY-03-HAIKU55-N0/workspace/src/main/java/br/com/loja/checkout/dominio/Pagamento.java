package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
