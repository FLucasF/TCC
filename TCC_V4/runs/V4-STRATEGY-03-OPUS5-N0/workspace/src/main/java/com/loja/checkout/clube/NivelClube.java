package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Para criar um nivel novo, basta criar um bean
 * que implemente esta interface, com o conjunto de vantagens dele.
 */
public interface NivelClube {

    /** Codigo do nivel (ex.: OURO). */
    String codigo();

    /** Credito para a proxima compra, sobre o valor dos produtos, em centavos. */
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    /** Se true, o frete sai zerado no resumo. */
    default boolean temFreteGratis() {
        return false;
    }

    /** Se true, o pedido vai com brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
