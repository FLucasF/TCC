package com.loja.pedidos.dto;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;

public class CriarPedidoRequest {
    @SerializedName("valorProdutos")
    private BigDecimal valorProdutos;

    @SerializedName("frete")
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
