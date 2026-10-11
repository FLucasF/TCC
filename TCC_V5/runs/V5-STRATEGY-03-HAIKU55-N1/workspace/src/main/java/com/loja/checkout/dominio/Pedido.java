package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemPedido> itens) {

    public BigDecimal subtotal() {
        BigDecimal soma = itens.stream().map(ItemPedido::total).reduce(Dinheiro.ZERO, BigDecimal::add);
        return Dinheiro.centavos(soma);
    }

    public BigDecimal pesoTotal() {
        return itens.stream().map(ItemPedido::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
