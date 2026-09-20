package com.loja.coupon;

import java.math.BigDecimal;
import java.util.List;
import com.loja.model.Item;
import com.loja.util.MoneyRounder;

public class Leve3Pague2Coupon implements Coupon {
    @Override
    public String getCode() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean isApplicable(BigDecimal productSubtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal productSubtotal, List<Item> items) {
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (Item item : items) {
            int quantity = item.getQuantidade();
            int freeItems = quantity / 3;
            BigDecimal itemPrice = new BigDecimal(String.valueOf(item.getPrecoUnitario()));
            BigDecimal itemDiscount = itemPrice.multiply(new BigDecimal(freeItems));
            totalDiscount = totalDiscount.add(itemDiscount);
        }

        return MoneyRounder.round(totalDiscount);
    }
}
