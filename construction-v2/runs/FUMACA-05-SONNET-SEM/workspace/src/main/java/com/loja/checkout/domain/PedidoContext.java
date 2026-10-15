package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Representa o pedido ja validado, com os totais base ja calculados,
 * usado pelas estrategias de entrega e de cupom para fazer seus calculos.
 */
public class PedidoContext {

    private final List<ItemPedido> itens;
    private final BigDecimal subtotalProdutos;
    private final BigDecimal pesoTotalKg;

    public PedidoContext(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {
        this.itens = itens;
        this.subtotalProdutos = subtotalProdutos;
        this.pesoTotalKg = pesoTotalKg;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public BigDecimal getSubtotalProdutos() {
        return subtotalProdutos;
    }

    public BigDecimal getPesoTotalKg() {
        return pesoTotalKg;
    }
}
