package com.loja.checkout.api;

public record ItemRequest(
        String nome,
        Double precoUnitario,
        Integer quantidade,
        Double pesoKg
) {}
