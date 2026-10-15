package com.loja.checkout.domain;

import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;
import java.util.List;

public record PedidoContexto(List<ItemPedido> itens, BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {

    public static PedidoContexto criar(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            peso = peso.add(item.pesoKg().multiply(quantidade));
            subtotal = subtotal.add(item.precoUnitario().multiply(quantidade));
        }
        return new PedidoContexto(itens, peso, Arredondamento.paraCentavos(subtotal));
    }
}
