package br.com.loja.checkout.clube;

import java.math.BigDecimal;

public record Vantagens(BigDecimal creditoProximaCompra, boolean freteGratis, boolean brinde) {
}
