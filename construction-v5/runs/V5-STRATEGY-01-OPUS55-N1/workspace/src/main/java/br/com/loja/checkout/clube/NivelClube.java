package br.com.loja.checkout.clube;

import java.math.BigDecimal;

/** Um nível do clube da loja e o conjunto de vantagens que ele dá. */
public interface NivelClube {

    String codigo();

    BigDecimal frete(BigDecimal freteDaModalidade);

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
