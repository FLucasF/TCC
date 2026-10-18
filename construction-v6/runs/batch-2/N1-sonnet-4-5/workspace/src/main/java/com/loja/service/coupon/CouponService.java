package com.loja.service.coupon;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouponService {
    private final Map<String, CouponStrategy> coupons = new HashMap<>();

    public CouponService() {
        List<CouponStrategy> strategies = List.of(
            new Welcome10Coupon(),
            new Minus50Coupon(),
            new FreeShippingCoupon(),
            new Buy3Pay2Coupon()
        );
        for (CouponStrategy strategy : strategies) {
            coupons.put(strategy.getCode(), strategy);
        }
    }

    public boolean exists(String couponCode) {
        return coupons.containsKey(couponCode);
    }

    public CouponStrategy getCoupon(String couponCode) {
        return coupons.get(couponCode);
    }

    public BigDecimal applyCap(BigDecimal discount, BigDecimal productsSubtotal) {
        BigDecimal maxDiscount = productsSubtotal.multiply(new BigDecimal("0.30"));
        if (discount.compareTo(maxDiscount) > 0) {
            return maxDiscount.setScale(2, java.math.RoundingMode.HALF_EVEN);
        }
        return discount;
    }
}
