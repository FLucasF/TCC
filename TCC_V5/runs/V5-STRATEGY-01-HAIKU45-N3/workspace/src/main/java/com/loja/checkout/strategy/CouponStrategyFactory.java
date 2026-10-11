package com.loja.checkout.strategy;

import com.loja.checkout.strategy.coupon.*;

public class CouponStrategyFactory {
    public static CouponStrategy create(String cupom) {
        return switch (cupom) {
            case "BEMVINDO10" -> new BemVindo10Coupon();
            case "MENOS50" -> new Menos50Coupon();
            case "FRETEGRATIS" -> new FreteGratisCoupon();
            case "LEVE3PAGUE2" -> new Leve3Pague2Coupon();
            default -> null;
        };
    }
}
