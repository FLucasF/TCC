package com.loja.checkout.dto;

public record ItemCarrinho(
    String nome,
    Double precoUnitario,
    Integer quantidade,
    Double pesoKg
) {}
