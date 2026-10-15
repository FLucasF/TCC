package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Região do cliente: só a porcentagem do seguro muda, a conta é a mesma. */
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

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.porcentagem(subtotalProdutos, percentualSeguro);
    }

    public static Optional<Regiao> de(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}
