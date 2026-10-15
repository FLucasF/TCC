package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum PaymentMethod {
    PIX {
        @Override public boolean validParcelas(int p) { return p == 1; }
        @Override public boolean isAvailable(BigDecimal total) { return true; }
        @Override
        public BigDecimal finalTotal(BigDecimal total, int p) {
            BigDecimal discount = total.multiply(PIX_RATE).setScale(2, RoundingMode.HALF_EVEN);
            return total.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override public BigDecimal installmentValue(BigDecimal total, int p) { return finalTotal(total, p); }
    },
    BOLETO {
        @Override public boolean validParcelas(int p) { return p == 1; }
        @Override
        public boolean isAvailable(BigDecimal total) {
            return total.compareTo(BOLETO_MAX) <= 0;
        }
        @Override public BigDecimal finalTotal(BigDecimal total, int p) { return total.add(BOLETO_FEE); }
        @Override public BigDecimal installmentValue(BigDecimal total, int p) { return finalTotal(total, p); }
    },
    CARTAO {
        @Override public boolean validParcelas(int p) { return p >= 1 && p <= 12; }
        @Override public boolean isAvailable(BigDecimal total) { return true; }
        @Override
        public BigDecimal finalTotal(BigDecimal total, int p) {
            if (p <= 3) return total;
            return installmentValue(total, p).multiply(BigDecimal.valueOf(p));
        }
        @Override
        public BigDecimal installmentValue(BigDecimal total, int p) {
            if (p <= 3) {
                return total.divide(BigDecimal.valueOf(p), 2, RoundingMode.HALF_EVEN);
            }
            BigDecimal onePlusRateN = BigDecimal.ONE.add(CARD_RATE).pow(p);
            BigDecimal factor = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(onePlusRateN, 15, RoundingMode.HALF_EVEN));
            return total.multiply(CARD_RATE)
                    .divide(factor, 15, RoundingMode.HALF_EVEN)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
    };

    private static final BigDecimal PIX_RATE = new BigDecimal("0.05");
    private static final BigDecimal BOLETO_FEE = new BigDecimal("3.49");
    private static final BigDecimal BOLETO_MAX = new BigDecimal("1000.00");
    private static final BigDecimal CARD_RATE = new BigDecimal("0.0199");

    public abstract boolean validParcelas(int parcelas);
    public abstract boolean isAvailable(BigDecimal orderTotal);
    public abstract BigDecimal finalTotal(BigDecimal orderTotal, int parcelas);
    public abstract BigDecimal installmentValue(BigDecimal orderTotal, int parcelas);
}
