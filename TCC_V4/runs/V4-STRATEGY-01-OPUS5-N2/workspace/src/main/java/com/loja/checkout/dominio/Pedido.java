package com.loja.checkout.dominio;

import com.loja.checkout.contrato.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente, ja' conferido. */
public record Pedido(List<Item> itens) {

    public static Pedido de(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(Pedido::item).toList());
    }

    private static Item item(ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new PedidoRecusadoException(ErroCheckout.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    /** A soma dos produtos: preco de cada item x quantidade. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.arredonda(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** O peso do pedido, sem arredondar. */
    public BigDecimal pesoKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
