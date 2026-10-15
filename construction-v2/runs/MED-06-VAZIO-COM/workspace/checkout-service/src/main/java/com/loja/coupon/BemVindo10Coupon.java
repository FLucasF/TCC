package com.loja.coupon;

import java.math.BigDecimal;
import java.util.List;
import com.loja.model.Item;
import com.loja.util.MoneyRounder;

public class BemVindo10Coupon implements Coupon {
    @Override
    public String getCode() {
        return "BEMVINDO10";
    }

    @Override
    public boolean isApplicable(BigDecimal productSubtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productSubtotal, List<Item> items) {
        return MoneyRounder.round(productSubtotal.multiply(new BigDecimal("0.10")));
    }
}
