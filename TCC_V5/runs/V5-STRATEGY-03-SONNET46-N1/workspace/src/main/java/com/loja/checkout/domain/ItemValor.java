package com.loja.checkout.domain;

import java.math.BigDecimal;

public record ItemValor(BigDecimal precoUnitario, int quantidade) {}
