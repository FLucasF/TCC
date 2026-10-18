package com.loja.strategy;

import com.loja.repository.OrderRepository;

public class CouponFactory {
    private final OrderRepository orderRepo;

    public CouponFactory(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    public CouponStrategy create(String couponCode) {
        if (couponCode == null || couponCode.isEmpty()) {
            return null;
        }

        return switch (couponCode) {
            case "WELCOME10" -> new Welcome10Coupon(orderRepo);
            case "MINUS50" -> new Minus50Coupon();
            case "FREESHIPPING" -> new FreeShippingCoupon();
            case "BUY3PAY2" -> new Buy3Pay2Coupon();
            default -> null;
        };
    }

    public boolean isValidCoupon(String couponCode) {
        return couponCode != null && switch (couponCode) {
            case "WELCOME10", "MINUS50", "FREESHIPPING", "BUY3PAY2" -> true;
            default -> false;
        };
    }
}
