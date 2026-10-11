package br.com.loja.checkout.clube;


import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    default BigDecimal frete(BigDecimal freteCalculado) {
        return freteCalculado;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
