package com.loja.checkout.dominio.seguro;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Regiao do cliente e o percentual que a seguradora cobra nela. A conta e a
 * mesma em todas: o percentual sobre o valor dos produtos, sem desconto e sem
 * frete.
 */
public enum Regiao {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal percentual;

    Regiao(String percentual) {
        this.percentual = new BigDecimal(percentual);
    }

    public BigDecimal percentual() {
        return percentual;
    }

    /** Seguro do envio: percentual da regiao sobre o valor dos produtos. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentual);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        return Arrays.stream(values()).filter(r -> r.name().equals(codigo)).findFirst();
    }
}
