package br.loja.checkout;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

/** Região do cliente e o percentual do seguro (sobre o valor dos produtos). */
public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }

    public static Optional<Regiao> de(String codigo) {
        return Stream.of(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}
