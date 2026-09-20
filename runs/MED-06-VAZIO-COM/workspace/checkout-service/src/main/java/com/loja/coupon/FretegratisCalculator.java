package com.loja.coupon;

import java.math.BigDecimal;

public class FretegratisCalculator {
    private static final String CODE = "FRETEGRATIS";

    public static boolean isFretegratis(String couponCode) {
        return CODE.equals(couponCode);
    }
}
