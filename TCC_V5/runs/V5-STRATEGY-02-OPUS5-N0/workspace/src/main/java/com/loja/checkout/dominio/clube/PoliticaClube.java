package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Nivel novo e implementacao nova anotada
 * com @Component, com o seu proprio conjunto de vantagens.
 */
public interface PoliticaClube extends Identificavel {

    /**
     * Credito para a proxima compra, sobre o valor dos produtos (sem desconto
     * e sem frete), arredondado para centavos. Nao abate nada nesta compra.
     */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** Se o nivel isenta o frete sempre. */
    boolean isentaFrete();

    /** Se o pedido ganha brinde. */
    boolean temBrinde(BigDecimal subtotalProdutos);
}
