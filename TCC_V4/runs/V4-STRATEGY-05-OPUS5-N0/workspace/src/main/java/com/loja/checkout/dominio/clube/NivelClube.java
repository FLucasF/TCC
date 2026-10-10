package com.loja.checkout.dominio.clube;

import com.loja.checkout.comum.Dinheiro;

import java.math.BigDecimal;

/**
 * Um nivel do clube da loja, com o conjunto de vantagens dele. Nivel novo e
 * uma classe nova implementando esta interface, marcada com @Component.
 */
public interface NivelClube {

    /** Codigo do nivel (ex.: OURO). */
    String codigo();

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Se o nivel nao paga frete. */
    default boolean temFreteGratis() {
        return false;
    }

    /** Se o pedido vai com brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
