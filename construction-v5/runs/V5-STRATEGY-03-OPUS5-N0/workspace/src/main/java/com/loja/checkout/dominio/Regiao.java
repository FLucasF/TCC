package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regiao do cliente. O seguro de envio e sempre a mesma conta - um percentual
 * sobre o valor dos produtos -, so o percentual muda de regiao para regiao.
 */
public enum Regiao {

    SUDESTE("1.0"),
    SUL("1.0"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2.0");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

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
