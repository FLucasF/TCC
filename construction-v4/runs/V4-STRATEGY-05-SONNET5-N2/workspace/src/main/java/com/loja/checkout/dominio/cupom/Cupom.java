package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Carrinho;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(Carrinho carrinho);

    /** @param frete frete já calculado, para cupons que descontam o frete. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
