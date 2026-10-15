package br.tcc.checkout.dto;

import java.math.BigDecimal;

public record Item(
    String nome,
    BigDecimal precoUnitario,
    Integer quantidade,
    BigDecimal pesoKg
) {}
