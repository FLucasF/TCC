package shop.domain.coupon;

import java.math.BigDecimal;
import shop.domain.DomainException;
import shop.domain.ErrorCode;

public class FreeShipping implements Coupon {
    @Override
    public String code() {
        return "FREESHIPPING";
    }

    @Override
    public BigDecimal discount(CouponContext c) {
        if (!c.shipping().chargesFreight()) {
            throw new DomainException(ErrorCode.COUPON_NOT_APPLICABLE);
        }
        return c.shippingCost();
    }
}
