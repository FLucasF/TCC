package com.loja.pedidos;

import java.math.BigDecimal;

public class CriarPedidoRequest {
    private BigDecimal valorProdutos;
    private BigDecimal frete;

    public BigDecimal getValorProdutos() {
        return valorProdutos;
    }

    public void setValorProdutos(BigDecimal valorProdutos) {
        this.valorProdutos = valorProdutos;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    public void setFrete(BigDecimal frete) {
        this.frete = frete;
    }
}
