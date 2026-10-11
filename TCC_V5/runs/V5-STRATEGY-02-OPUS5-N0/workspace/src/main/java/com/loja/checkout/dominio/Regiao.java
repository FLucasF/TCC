package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. O seguro de extravio e roubo e sempre a mesma conta
 * (percentual sobre o valor dos produtos); so o percentual muda por regiao.
 */
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

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    /** Seguro do envio: percentual da regiao sobre os produtos, sem desconto e sem frete. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }

    public static Optional<Regiao> porNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        for (Regiao regiao : values()) {
            if (regiao.name().equals(nome)) {
                return Optional.of(regiao);
            }
        }
        return Optional.empty();
    }
}
