package shop.domain.coupon;

import java.math.BigDecimal;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.domain.Money;

public class Minus50 implements Coupon {
    private static final BigDecimal MINIMUM = Money.of("300.00");

    @Override
    public String code() {
        return "MINUS50";
    }

    @Override
    public BigDecimal discount(CouponContext c) {
        if (c.productsSubtotal().compareTo(MINIMUM) < 0) {
            throw new DomainException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        return Money.of("50.00");
    }
}
