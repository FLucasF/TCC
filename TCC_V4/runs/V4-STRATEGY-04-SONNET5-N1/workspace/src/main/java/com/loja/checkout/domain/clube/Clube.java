package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Codificavel;

import java.math.BigDecimal;

public interface Clube extends Codificavel {

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean concedeBrinde(BigDecimal subtotalProdutos);
}
