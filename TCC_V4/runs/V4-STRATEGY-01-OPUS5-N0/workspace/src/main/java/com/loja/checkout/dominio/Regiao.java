package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. Muda apenas o percentual que a seguradora cobra;
 * a conta do seguro e a mesma em todas: percentual sobre o valor dos produtos.
 */
public enum Regiao implements Codificavel {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    @Override
    public String codigo() {
        return name();
    }

    /** Seguro contra extravio e roubo: percentual da regiao sobre os produtos, em centavos. */
    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return Moeda.percentual(subtotalProdutos, taxaSeguro);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        for (Regiao regiao : values()) {
            if (regiao.codigo().equals(codigo)) {
                return Optional.of(regiao);
            }
        }
        return Optional.empty();
    }
}
