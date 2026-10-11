package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube. A loja estuda criar mais niveis, cada um com seu
 * conjunto de vantagens: credito de volta, isencao de frete e brinde. Cada nivel
 * e uma implementacao que decide as tres coisas.
 */
public interface NivelClube {

    /** O codigo do nivel (ex.: OURO). */
    String codigo();

    /** Credito para a proxima compra sobre o valor dos produtos, arredondado. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** Frete que o cliente de fato paga; alguns niveis isentam. */
    BigDecimal aplicarFrete(BigDecimal frete);

    /** Se o pedido ganha brinde. */
    boolean brinde(BigDecimal subtotalProdutos);
}
