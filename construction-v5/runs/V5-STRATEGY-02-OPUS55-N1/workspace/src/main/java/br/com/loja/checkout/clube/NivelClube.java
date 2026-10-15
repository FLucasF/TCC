package br.com.loja.checkout.clube;

import java.math.BigDecimal;

/** Cada nível do clube define seu conjunto de vantagens. */
public interface NivelClube {

    String codigo();

    BigDecimal credito(BigDecimal subtotalProdutos);

    /** Frete que o cliente paga, a partir do frete calculado pela entrega. */
    BigDecimal frete(BigDecimal freteCalculado);

    boolean brinde(BigDecimal subtotalProdutos);
}
