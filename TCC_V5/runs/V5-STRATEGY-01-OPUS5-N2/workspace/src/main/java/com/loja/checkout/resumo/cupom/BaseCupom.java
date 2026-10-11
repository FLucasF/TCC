package com.loja.checkout.resumo.cupom;

import com.loja.checkout.resumo.Item;
import java.math.BigDecimal;
import java.util.List;

/** O que um cupom pode olhar do pedido para decidir e calcular o desconto. */
public record BaseCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
