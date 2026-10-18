package com.loja.service.coupon;

import com.loja.domain.CouponCode;
import com.loja.domain.Money;
import com.loja.domain.OrderItem;
import com.loja.domain.Product;
import org.springframework.stereotype.Component;

@Component
public class Buy3Pay2Coupon implements CouponStrategy {

    @Override
    public CouponCode getCode() {
        return CouponCode.BUY3PAY2;
    }

    @Override
    public boolean isApplicable(CouponContext context) {
        return true;
    }

    @Override
    public Money calculateDiscount(CouponContext context) {
        Money totalDiscount = Money.zero();

        for (OrderItem item : context.items()) {
            if (item.getQuantity() >= 3) {
                Product product = context.products().get(item.getSku());
                int freeUnits = item.getQuantity() / 3;
                Money unitDiscount = product.getPrice().multiply(freeUnits);
                totalDiscount = totalDiscount.add(unitDiscount);
            }
        }

        return totalDiscount;
    }
}
