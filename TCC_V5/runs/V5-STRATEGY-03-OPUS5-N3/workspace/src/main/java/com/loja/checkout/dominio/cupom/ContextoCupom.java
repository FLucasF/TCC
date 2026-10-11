package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Carrinho;

import java.math.BigDecimal;

/**
 * O que um cupom pode olhar para decidir o desconto: o carrinho e o frete já cobrado
 * (FRETEGRATIS devolve o frete, LEVE3PAGUE2 olha item por item, MENOS50 olha o subtotal).
 */
public record ContextoCupom(Carrinho carrinho, BigDecimal frete) {

    public BigDecimal subtotal() {
        return carrinho.subtotal();
    }
}
