package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Como o pedido termina de ser pago: valor final e valor de cada parcela. */
public record Pago(BigDecimal totalFinal, BigDecimal valorParcela) {
}
