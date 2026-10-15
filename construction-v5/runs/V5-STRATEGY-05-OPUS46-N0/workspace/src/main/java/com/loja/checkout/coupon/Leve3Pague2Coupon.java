package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class Leve3Pague2Coupon implements CouponStrategy {

    @Override
    public String getCode() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean isApplicable(List<ItemRequest> items, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(List<ItemRequest> items, BigDecimal subtotal, BigDecimal shippingCost) {
        BigDecimal discount = BigDecimal.ZERO;
        for (ItemRequest item : items) {
            int freeUnits = item.quantidade() / 3;
            if (freeUnits > 0) {
                discount = discount.add(item.precoUnitario().multiply(new BigDecimal(freeUnits)));
            }
        }
        return discount.setScale(2, RoundingMode.HALF_EVEN);
    }
}
