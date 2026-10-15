package com.loja.checkout.dto;

import com.loja.checkout.model.Item;

import java.math.BigDecimal;

public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    public Item paraModelo() {
        return new Item(nome, precoUnitario, quantidade, pesoKg);
    }
}
