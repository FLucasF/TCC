package com.loja.checkout.dto;

public record ItemDto(
        String nome,
        Double precoUnitario,
        Integer quantidade,
        Double pesoKg
) {}
