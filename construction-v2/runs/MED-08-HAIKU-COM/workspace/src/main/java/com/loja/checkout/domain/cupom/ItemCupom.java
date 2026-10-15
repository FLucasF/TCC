package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public record ItemCupom(String nome, BigDecimal precoUnitario, int quantidade) {
}
