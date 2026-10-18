package shop.payment;

import java.math.BigDecimal;

public record PaymentCharge(BigDecimal finalTotal, BigDecimal installmentAmount) {
}
