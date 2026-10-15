package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

/** Tudo que um cupom pode precisar olhar para decidir o desconto. */
public record PedidoParaCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
