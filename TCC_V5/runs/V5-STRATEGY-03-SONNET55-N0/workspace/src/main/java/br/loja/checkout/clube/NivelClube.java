package br.loja.checkout.clube;

import java.math.BigDecimal;

/** Um nível do clube e suas vantagens. Para criar um novo, basta implementar e anotar com @Component. */
public interface NivelClube {
    String codigo();

    /** Percentual do valor dos produtos devolvido como crédito (ex.: 0.02 para 2%). */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
