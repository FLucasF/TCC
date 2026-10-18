package br.com.loja.pedidos.payment;

import java.math.BigDecimal;

public record PaymentCharge(BigDecimal finalTotal, BigDecimal installmentAmount) {
}
