package com.loja.dominio;

import java.math.BigDecimal;

/** Um nível do clube e suas vantagens. Para criar um novo, basta uma nova classe @Component. */
public interface NivelClube {

    String codigo();

    /** Porcentagem (ex.: "0.02") dos produtos devolvida em crédito. */
    String taxaCredito();

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
