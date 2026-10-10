package br.com.loja.checkout.cupom;

import br.com.loja.checkout.calculo.Item;
import java.math.BigDecimal;
import java.util.List;

/** O que um cupom pode olhar do pedido para decidir o desconto. */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
