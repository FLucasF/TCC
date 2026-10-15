package br.com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho já validado. */
public record Pedido(List<ItemPedido> itens) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredondar(itens.stream().map(ItemPedido::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Soma de peso × quantidade de cada item, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(ItemPedido::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
