package br.tcc.checkout.dominio;

import java.math.BigDecimal;

/** O que o cliente vai pagar de fato, depois do ajuste da forma de pagamento. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
