package br.com.marealta.dominio;

import java.math.BigDecimal;

/** Resultado do cálculo de preço de um plano: soma das noites, desconto e total. */
public record Cobranca(BigDecimal diarias, BigDecimal desconto) {

    public BigDecimal total() {
        return diarias.subtract(desconto);
    }
}
