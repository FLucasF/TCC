package shop.domain;

import java.math.BigDecimal;
import java.math.MathContext;

public interface PaymentMethod {
    record Settlement(BigDecimal finalTotal, BigDecimal installmentAmount) {
    }

    String code();

    boolean acceptsInstallments(int installments);

    /** Refuses with PAYMENT_METHOD_UNAVAILABLE when the method cannot be used for this total. */
    default void checkAvailable(BigDecimal orderTotal) {
    }

    Settlement settle(BigDecimal orderTotal, int installments, boolean couponUsed);

    record Pix(String code, BigDecimal discountRate) implements PaymentMethod {
        @Override
        public boolean acceptsInstallments(int n) {
            return n == 1;
        }

        @Override
        public Settlement settle(BigDecimal total, int n, boolean couponUsed) {
            BigDecimal discount = couponUsed ? Money.ZERO : Money.round(total.multiply(discountRate));
            BigDecimal result = total.subtract(discount);
            return new Settlement(result, result);
        }
    }

    record BankSlip(String code, BigDecimal fee, BigDecimal maximumTotal) implements PaymentMethod {
        @Override
        public boolean acceptsInstallments(int n) {
            return n == 1;
        }

        @Override
        public void checkAvailable(BigDecimal total) {
            if (total.compareTo(maximumTotal) > 0) {
                throw ApiException.unprocessable("PAYMENT_METHOD_UNAVAILABLE");
            }
        }

        @Override
        public Settlement settle(BigDecimal total, int n, boolean couponUsed) {
            BigDecimal result = total.add(fee);
            return new Settlement(result, result);
        }
    }

    /** Interest-free up to {@code freeUntil} installments, then Price table. */
    record CreditCard(String code, int maxInstallments, int freeUntil, BigDecimal monthlyRate)
            implements PaymentMethod {
        @Override
        public boolean acceptsInstallments(int n) {
            return n >= 1 && n <= maxInstallments;
        }

        @Override
        public Settlement settle(BigDecimal total, int n, boolean couponUsed) {
            if (n <= freeUntil) {
                return new Settlement(total, Money.round(total.divide(BigDecimal.valueOf(n), MathContext.DECIMAL128)));
            }
            BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(n, MathContext.DECIMAL128);
            // total * i / (1 - (1+i)^-n)  ==  total * i * (1+i)^n / ((1+i)^n - 1)
            BigDecimal installment = Money.round(total.multiply(monthlyRate).multiply(growth)
                    .divide(growth.subtract(BigDecimal.ONE), MathContext.DECIMAL128));
            return new Settlement(installment.multiply(BigDecimal.valueOf(n)), installment);
        }
    }
}
