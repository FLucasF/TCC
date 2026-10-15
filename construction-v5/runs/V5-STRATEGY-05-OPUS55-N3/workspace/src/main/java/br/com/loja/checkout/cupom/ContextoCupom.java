package br.com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import br.com.loja.checkout.ItemPedido;

public record ContextoCupom(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
