package shop;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

record PaymentOutcome(BigDecimal finalTotal, BigDecimal installmentAmount) {}

/** Uma forma de pagamento: cada nova forma é uma nova implementação registrada em {@link PaymentMethods}. */
interface PaymentMethod {
    String code();

    boolean acceptsInstallments(int installments);

    boolean availableFor(BigDecimal orderTotal);

    PaymentOutcome settle(BigDecimal orderTotal, int installments, boolean couponUsed);
}

final class PaymentMethods {
    private final Map<String, PaymentMethod> byCode = new LinkedHashMap<>();

    PaymentMethods(PaymentMethod... methods) {
        for (PaymentMethod m : methods) byCode.put(m.code(), m);
    }

    static PaymentMethods standard() {
        return new PaymentMethods(new Pix(), new CreditCard(), new BankSlip());
    }

    PaymentMethod get(String code) {
        return code == null ? null : byCode.get(code);
    }

    static final class Pix implements PaymentMethod {
        public String code() { return "PIX"; }

        public boolean acceptsInstallments(int n) { return n == 1; }

        public boolean availableFor(BigDecimal total) { return true; }

        public PaymentOutcome settle(BigDecimal total, int n, boolean couponUsed) {
            BigDecimal result = couponUsed ? total : total.subtract(Money.percent(total, "0.05"));
            return new PaymentOutcome(result, result);
        }
    }

    static final class BankSlip implements PaymentMethod {
        private static final BigDecimal FEE = new BigDecimal("3.49");
        private static final BigDecimal MAX_TOTAL = new BigDecimal("1000.00");

        public String code() { return "BANK_SLIP"; }

        public boolean acceptsInstallments(int n) { return n == 1; }

        public boolean availableFor(BigDecimal total) { return total.compareTo(MAX_TOTAL) <= 0; }

        public PaymentOutcome settle(BigDecimal total, int n, boolean couponUsed) {
            BigDecimal result = total.add(FEE);
            return new PaymentOutcome(result, result);
        }
    }

    static final class CreditCard implements PaymentMethod {
        private static final int MAX_INTEREST_FREE = 3;
        private static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0199");
        private static final MathContext MC = new MathContext(40);

        public String code() { return "CREDIT_CARD"; }

        public boolean acceptsInstallments(int n) { return n >= 1 && n <= 12; }

        public boolean availableFor(BigDecimal total) { return true; }

        public PaymentOutcome settle(BigDecimal total, int n, boolean couponUsed) {
            BigDecimal count = BigDecimal.valueOf(n);
            if (n <= MAX_INTEREST_FREE) {
                return new PaymentOutcome(total, total.divide(count, 2, RoundingMode.HALF_EVEN));
            }
            // Tabela Price: parcela = total * i / (1 - (1 + i)^-n)
            BigDecimal growth = BigDecimal.ONE.add(MONTHLY_RATE).pow(n, MC);
            BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growth, MC), MC);
            BigDecimal installment = Money.round(total.multiply(MONTHLY_RATE).divide(denominator, MC));
            return new PaymentOutcome(installment.multiply(count), installment);
        }
    }
}
