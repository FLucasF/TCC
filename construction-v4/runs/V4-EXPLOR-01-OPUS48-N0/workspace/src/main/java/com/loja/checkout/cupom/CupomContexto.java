package com.loja.checkout.cupom;

import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

/**
 * Tudo o que um cupom pode precisar para decidir se se aplica e quanto desconta.
 *
 * @param itens             itens do carrinho
 * @param subtotalProdutos  soma dos produtos (já arredondada)
 * @param frete             frete do pedido (já arredondado) — usado pelo FRETEGRATIS
 */
public record CupomContexto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
