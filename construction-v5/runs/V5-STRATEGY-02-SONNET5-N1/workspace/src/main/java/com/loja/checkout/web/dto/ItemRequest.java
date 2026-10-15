package com.loja.checkout.web.dto;

public record ItemRequest(
        String nome,
        Double precoUnitario,
        Integer quantidade,
        Double pesoKg
) {
}
