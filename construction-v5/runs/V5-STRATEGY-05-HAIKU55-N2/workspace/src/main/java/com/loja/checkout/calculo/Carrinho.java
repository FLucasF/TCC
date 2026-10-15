package com.loja.checkout.calculo;

import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.calculo.Dinheiro.ZERO;

record Carrinho(List<ItemCarrinho> itens) {

    static Carrinho validar(List<ItemRequest> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Carrinho(pedidos.stream().map(Carrinho::validarItem).toList());
    }

    private static ItemCarrinho validarItem(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new ItemCarrinho(item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    BigDecimal subtotal() {
        return itens.stream().map(ItemCarrinho::total).reduce(ZERO, BigDecimal::add);
    }

    BigDecimal pesoKg() {
        return itens.stream().map(ItemCarrinho::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
