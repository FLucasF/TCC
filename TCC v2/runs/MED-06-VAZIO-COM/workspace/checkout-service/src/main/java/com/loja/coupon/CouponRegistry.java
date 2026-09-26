package com.loja.coupon;

import java.util.HashMap;
import java.util.Map;

public class CouponRegistry {
    private static final Map<String, Coupon> coupons = new HashMap<>();

    static {
        coupons.put("BEMVINDO10", new BemVindo10Coupon());
        coupons.put("MENOS50", new Menos50Coupon());
        coupons.put("LEVE3PAGUE2", new Leve3Pague2Coupon());
    }

    public static Coupon getCoupon(String code) {
        return coupons.get(code);
    }
}
