package shop.coupon;

import shop.Money;
import shop.checkout.CartLine;

import java.math.BigDecimal;

public final class Buy3Pay2Coupon implements Coupon {
    @Override
    public String code() {
        return "BUY3PAY2";
    }

    @Override
    public BigDecimal discount(CouponContext context) {
        BigDecimal discount = Money.ZERO;
        for (CartLine line : context.lines()) {
            discount = discount.add(line.product().price().multiply(BigDecimal.valueOf(line.quantity() / 3)));
        }
        return discount;
    }
}
