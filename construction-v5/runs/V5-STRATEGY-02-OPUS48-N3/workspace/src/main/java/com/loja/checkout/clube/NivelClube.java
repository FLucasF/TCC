package com.loja.checkout.clube;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Um nível do clube. Cada nível tem seu conjunto de vantagens: crédito para a
 * próxima compra, frete grátis e brinde. Nível novo = uma classe nova.
 */
public interface NivelClube extends Codificavel {

    /** Crédito para a próxima compra, já arredondado, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** Se este nível nunca paga frete. */
    boolean freteGratis();

    /** Se o pedido ganha brinde, conforme o valor dos produtos. */
    boolean brinde(BigDecimal subtotalProdutos);
}
