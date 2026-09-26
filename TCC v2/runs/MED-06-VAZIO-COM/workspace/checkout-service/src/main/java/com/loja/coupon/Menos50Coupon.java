package com.loja.coupon;

import java.math.BigDecimal;
import java.util.List;
import com.loja.model.Item;

public class Menos50Coupon implements Coupon {
    @Override
    public String getCode() {
        return "MENOS50";
    }

    @Override
    public boolean isApplicable(BigDecimal productSubtotal) {
        return productSubtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productSubtotal, List<Item> items) {
        return new BigDecimal("50.00");
    }
}
