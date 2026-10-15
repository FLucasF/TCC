package com.example.checkout.model;

import java.math.BigDecimal;

public record Item(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {}
