package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. A conta do seguro de envio e a mesma para todas as regioes
 * (percentual sobre o valor dos produtos), so o percentual muda.
 */
public enum Regiao {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    /** Seguro contra extravio e roubo: percentual da regiao sobre os produtos, sem desconto e sem frete. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        for (Regiao regiao : values()) {
            if (regiao.name().equals(codigo)) {
                return Optional.of(regiao);
            }
        }
        return Optional.empty();
    }
}
