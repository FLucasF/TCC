package com.loja.coupon;

import java.math.BigDecimal;

public interface Coupon {
    String getCode();
    boolean isApplicable(BigDecimal productSubtotal);
    BigDecimal calculateDiscount(BigDecimal productSubtotal, java.util.List<com.loja.model.Item> items);
}
