package com.loja.checkout.clube;

import com.loja.checkout.dominio.Identificado;

import java.math.BigDecimal;

/** Um nível do clube da loja, com o conjunto de vantagens que ele dá ao cliente. */
public interface NivelClube extends Identificado {

    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
