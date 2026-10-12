package com.loja.checkout.clube;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Nível do cliente no clube. O enunciado diz que estão estudando criar mais
 * níveis e cada um terá seu conjunto de vantagens, então cada nível mora numa
 * classe própria com suas três vantagens possíveis:
 *  - crédito para a próxima compra (sobre os produtos, sem desconto e sem frete);
 *  - isenção de frete;
 *  - brinde.
 */
public interface NivelClube extends Codificavel {

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
