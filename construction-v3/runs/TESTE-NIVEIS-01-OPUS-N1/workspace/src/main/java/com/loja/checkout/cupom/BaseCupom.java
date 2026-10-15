package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;

import java.math.BigDecimal;

/**
 * O que um cupom pode olhar para decidir e para calcular: os produtos do
 * carrinho e o frete do pedido.
 */
public record BaseCupom(Carrinho carrinho, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return carrinho.subtotalProdutos();
    }
}
