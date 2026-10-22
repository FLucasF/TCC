package br.com.marealta.plan;

import java.math.BigDecimal;

/** Resultado do cálculo de preço: subtotal (soma das noites) e desconto. */
public record Quote(int nights, BigDecimal subtotal, BigDecimal discount) {
    public BigDecimal total() { return subtotal.subtract(discount); }
}
