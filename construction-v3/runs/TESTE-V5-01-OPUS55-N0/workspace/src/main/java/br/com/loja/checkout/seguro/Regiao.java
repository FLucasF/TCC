package br.com.loja.checkout.seguro;

import br.com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/** Regiões atendidas e a porcentagem que a seguradora cobra em cada uma. */
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

    /** Seguro contra extravio e roubo: porcentagem sobre o valor dos produtos, sem desconto e sem frete. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaSeguro);
    }

    public static Optional<Regiao> buscar(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}
