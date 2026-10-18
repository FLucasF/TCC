package shop.domain.coupon;

import java.math.BigDecimal;
import shop.domain.CartLine;

public class Buy3Pay2 implements Coupon {
    @Override
    public String code() {
        return "BUY3PAY2";
    }

    @Override
    public BigDecimal discount(CouponContext c) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartLine line : c.lines()) {
            total = total.add(line.product().price().multiply(BigDecimal.valueOf(line.quantity() / 3)));
        }
        return total;
    }
}
