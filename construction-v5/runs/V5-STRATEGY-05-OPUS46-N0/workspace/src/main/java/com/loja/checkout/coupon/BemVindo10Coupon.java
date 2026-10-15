package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class BemVindo10Coupon implements CouponStrategy {

    private static final BigDecimal RATE = new BigDecimal("0.10");

    @Override
    public String getCode() {
        return "BEMVINDO10";
    }

    @Override
    public boolean isApplicable(List<ItemRequest> items, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(List<ItemRequest> items, BigDecimal subtotal, BigDecimal shippingCost) {
        return subtotal.multiply(RATE).setScale(2, RoundingMode.HALF_EVEN);
    }
}
