package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * O carrinho já validado e somado.
 *
 * @param subtotalProdutos soma dos produtos (preço x quantidade), em centavos
 * @param pesoTotal        peso do pedido (peso x quantidade), SEM arredondar
 * @param itens            os itens, para cupons que olham item a item
 */
public record Pedido(BigDecimal subtotalProdutos, BigDecimal pesoTotal, List<Item> itens) {

    public record Item(BigDecimal precoUnitario, int quantidade) {
    }
}
