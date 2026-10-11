package com.loja.checkout.model;

public record ItemCarrinho(
        String nome,
        Double precoUnitario,
        Integer quantidade,
        Double pesoKg
) {}
