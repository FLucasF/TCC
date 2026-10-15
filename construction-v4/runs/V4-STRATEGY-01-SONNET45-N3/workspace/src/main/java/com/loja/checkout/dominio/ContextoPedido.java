package com.loja.checkout.dominio;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class ContextoPedido {
    private final List<ItemCarrinho> itens;
    private BigDecimal subtotalProdutos;
    private BigDecimal pesoTotal;

    public ContextoPedido(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public BigDecimal getSubtotalProdutos() {
        return subtotalProdutos;
    }

    public void setSubtotalProdutos(BigDecimal subtotalProdutos) {
        this.subtotalProdutos = subtotalProdutos;
    }

    public BigDecimal getPesoTotal() {
        return pesoTotal;
    }

    public void setPesoTotal(BigDecimal pesoTotal) {
        this.pesoTotal = pesoTotal;
    }
}
