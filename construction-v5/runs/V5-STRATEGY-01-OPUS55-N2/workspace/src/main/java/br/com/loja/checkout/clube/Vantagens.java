package br.com.loja.checkout.clube;

import java.math.BigDecimal;

public record Vantagens(BigDecimal credito, boolean isentaFrete, boolean brinde) {
}
