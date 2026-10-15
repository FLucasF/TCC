package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Só a porcentagem do seguro muda por região; a conta é a mesma. */
public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(taxaSeguro));
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}
