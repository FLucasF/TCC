package com.loja.pedidos.dto;

public class ItemDTO {
    public String sku;
    public int quantidade;

    public ItemDTO() {}

    public ItemDTO(String sku, int quantidade) {
        this.sku = sku;
        this.quantidade = quantidade;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}
