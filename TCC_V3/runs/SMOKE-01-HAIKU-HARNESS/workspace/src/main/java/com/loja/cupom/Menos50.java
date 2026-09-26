package com.loja.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.loja.model.Item;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal DISCOUNT_VALUE = new BigDecimal("50.00");
    private static final BigDecimal MIN_SUBTOTAL = new BigDecimal("300.00");

    @Override
    public boolean isApplicable(List<Item> itens, BigDecimal subtotal) {
        return subtotal.compareTo(MIN_SUBTOTAL) >= 0;
    }

    @Override
    public BigDecimal calculateDiscount(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return DISCOUNT_VALUE.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getCode() {
        return "MENOS50";
    }
}
