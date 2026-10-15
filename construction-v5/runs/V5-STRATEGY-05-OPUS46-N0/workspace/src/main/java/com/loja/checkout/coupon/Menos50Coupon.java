package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Menos50Coupon implements CouponStrategy {

    private static final BigDecimal DISCOUNT = new BigDecimal("50.00");
    private static final BigDecimal MIN_SUBTOTAL = new BigDecimal("300.00");

    @Override
    public String getCode() {
        return "MENOS50";
    }

    @Override
    public boolean isApplicable(List<ItemRequest> items, BigDecimal subtotal) {
        return subtotal.compareTo(MIN_SUBTOTAL) >= 0;
    }

    @Override
    public BigDecimal calculateDiscount(List<ItemRequest> items, BigDecimal subtotal, BigDecimal shippingCost) {
        return DISCOUNT;
    }
}
