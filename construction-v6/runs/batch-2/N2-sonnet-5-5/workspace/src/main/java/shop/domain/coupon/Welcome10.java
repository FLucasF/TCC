package shop.domain.coupon;

import java.math.BigDecimal;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.domain.Money;

public class Welcome10 implements Coupon {
    @Override
    public String code() {
        return "WELCOME10";
    }

    @Override
    public BigDecimal discount(CouponContext c) {
        if (!c.firstPurchase()) {
            throw new DomainException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        return Money.percent(c.productsSubtotal(), BigDecimal.TEN);
    }
}
