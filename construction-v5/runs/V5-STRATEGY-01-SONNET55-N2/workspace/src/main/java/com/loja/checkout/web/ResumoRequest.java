package com.loja.checkout.web;

import com.loja.checkout.PedidoEntrada;
import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.PedidoRecusadoException;
import java.math.BigDecimal;
import java.util.List;

public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {

    public PedidoEntrada paraPedido() {
        return new PedidoEntrada(carrinho(), modalidadeEntrega, cupom, formaPagamento, parcelas, nivelClube, regiao);
    }

    private Carrinho carrinho() {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream().map(ResumoRequest::item).toList());
    }

    private static Item item(ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new PedidoRecusadoException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
