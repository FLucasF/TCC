package com.loja.checkout.domain;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class ContextoCalculo {
    private final List<ItemCarrinho> itens;
    private BigDecimal subtotalProdutos;
    private BigDecimal frete;

    public ContextoCalculo(List<ItemCarrinho> itens) {
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

    public BigDecimal getFrete() {
        return frete;
    }

    public void setFrete(BigDecimal frete) {
        this.frete = frete;
    }

    public BigDecimal getPesoTotal() {
        return itens.stream()
            .map(ItemCarrinho::pesoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
