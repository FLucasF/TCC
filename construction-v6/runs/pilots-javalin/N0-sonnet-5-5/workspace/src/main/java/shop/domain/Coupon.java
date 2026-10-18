package shop.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BooleanSupplier;

public interface Coupon {
    record Context(String cpf, List<CartLine> lines, BigDecimal subtotal, ShippingMethod shipping,
                   BigDecimal shippingCost, BooleanSupplier firstPurchase) {
    }

    String code();

    /** Discount before the 30% cap; refuses with COUPON_NOT_APPLICABLE when the order does not qualify. */
    BigDecimal discount(Context context);

    static ApiException notApplicable() {
        return ApiException.unprocessable("COUPON_NOT_APPLICABLE");
    }

    record PercentOfProducts(String code, BigDecimal rate, boolean firstPurchaseOnly) implements Coupon {
        @Override
        public BigDecimal discount(Context c) {
            if (firstPurchaseOnly && !c.firstPurchase().getAsBoolean()) {
                throw notApplicable();
            }
            return Money.round(c.subtotal().multiply(rate));
        }
    }

    record FixedOffProducts(String code, BigDecimal amount, BigDecimal minimumSubtotal) implements Coupon {
        @Override
        public BigDecimal discount(Context c) {
            if (c.subtotal().compareTo(minimumSubtotal) < 0) {
                throw notApplicable();
            }
            return amount;
        }
    }

    record FreeShipping(String code) implements Coupon {
        @Override
        public BigDecimal discount(Context c) {
            if (!c.shipping().chargesFreight()) {
                throw notApplicable();
            }
            return c.shippingCost();
        }
    }

    /** Every {@code groupSize}-th unit of the same product is free. */
    record EveryNthFree(String code, int groupSize) implements Coupon {
        @Override
        public BigDecimal discount(Context c) {
            BigDecimal total = Money.ZERO;
            for (CartLine line : c.lines()) {
                int free = line.quantity() / groupSize;
                total = total.add(line.product().price().multiply(BigDecimal.valueOf(free)));
            }
            return Money.round(total);
        }
    }
}
