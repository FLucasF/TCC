package com.loja.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.loja.model.Item;
import java.util.List;

public class Bemvindo10 implements Cupom {
    private static final BigDecimal DISCOUNT_PERCENTAGE = new BigDecimal("0.10");

    @Override
    public boolean isApplicable(List<Item> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return subtotal.multiply(DISCOUNT_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getCode() {
        return "BEMVINDO10";
    }
}
