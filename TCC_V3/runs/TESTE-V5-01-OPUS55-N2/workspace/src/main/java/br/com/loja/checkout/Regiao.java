package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Região do cliente; só muda a porcentagem do seguro cobrada pela seguradora. */
public enum Regiao {
    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final String percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal seguro(Carrinho carrinho) {
        return Dinheiro.percentual(carrinho.subtotal(), percentualSeguro);
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(codigo)).findFirst();
    }
}
