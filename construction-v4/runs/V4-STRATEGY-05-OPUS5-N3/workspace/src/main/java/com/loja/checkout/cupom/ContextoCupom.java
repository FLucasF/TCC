package com.loja.checkout.cupom;

import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

/**
 * O que um cupom pode olhar para decidir o desconto: os itens do carrinho, o
 * valor dos produtos e o frete ja calculado.
 */
public record ContextoCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
