package br.com.loja.checkout.cupom;

import br.com.loja.checkout.pedido.Item;
import java.math.BigDecimal;
import java.util.List;

/** O que qualquer cupom pode olhar do pedido para dizer quanto abate. */
public record ContextoCupom(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
