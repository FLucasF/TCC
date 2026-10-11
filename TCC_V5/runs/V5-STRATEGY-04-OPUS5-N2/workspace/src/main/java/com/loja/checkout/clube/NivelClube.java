package com.loja.checkout.clube;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/** Um nivel do clube da loja: o conjunto de vantagens que o cliente recebe. */
public interface NivelClube extends Codificado {

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
