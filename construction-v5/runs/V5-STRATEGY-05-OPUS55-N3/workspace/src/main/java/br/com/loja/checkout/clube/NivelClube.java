package br.com.loja.checkout.clube;

import java.math.BigDecimal;

import br.com.loja.checkout.Codificado;

public interface NivelClube extends Codificado {

    BigDecimal freteCobrado(BigDecimal frete);

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
