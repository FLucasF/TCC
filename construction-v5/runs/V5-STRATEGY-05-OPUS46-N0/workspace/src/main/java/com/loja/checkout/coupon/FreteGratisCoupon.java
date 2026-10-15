package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FreteGratisCoupon implements CouponStrategy {

    @Override
    public String getCode() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean isApplicable(List<ItemRequest> items, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(List<ItemRequest> items, BigDecimal subtotal, BigDecimal shippingCost) {
        return shippingCost;
    }
}
