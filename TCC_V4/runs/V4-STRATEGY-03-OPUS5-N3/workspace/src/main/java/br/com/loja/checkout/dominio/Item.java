package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto do carrinho, com a quantidade pedida. */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public Dinheiro total() {
        return Dinheiro.de(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Peso das unidades deste item, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
