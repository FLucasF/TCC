package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CouponStrategy {

    String getCode();

    boolean isApplicable(List<ItemRequest> items, BigDecimal subtotal);

    BigDecimal calculateDiscount(List<ItemRequest> items, BigDecimal subtotal, BigDecimal shippingCost);
}
