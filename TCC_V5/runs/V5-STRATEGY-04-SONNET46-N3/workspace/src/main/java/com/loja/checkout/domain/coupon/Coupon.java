package com.loja.checkout.domain.coupon;

import com.loja.checkout.domain.CartItem;

import java.math.BigDecimal;
import java.util.List;

public interface Coupon {
    BigDecimal discount(BigDecimal subtotal, List<CartItem> items, BigDecimal frete);
    boolean isApplicable(BigDecimal subtotal, List<CartItem> items);
}
