package com.loja.checkout.dto;

public record ItemPedido(
    String nome,
    Double precoUnitario,
    Integer quantidade,
    Double pesoKg
) {}
