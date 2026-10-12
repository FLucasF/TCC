package br.com.loja.checkout.pedido;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.PedidoRecusadoException;

import java.math.BigDecimal;
import java.util.List;

public record Carrinho(List<Item> itens, BigDecimal subtotal, BigDecimal pesoKg) {

    public static Carrinho de(List<Item> itens) {
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(item -> item != null && item.valido())) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
        BigDecimal subtotal = itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(Item::pesoTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Carrinho(List.copyOf(itens), Dinheiro.centavos(subtotal), peso);
    }
}
