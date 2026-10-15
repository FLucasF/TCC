package com.loja.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.loja.model.Item;
import java.util.List;

public class FretegratisCupom implements Cupom {
    @Override
    public boolean isApplicable(List<Item> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calculateDiscount(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return frete.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String getCode() {
        return "FRETEGRATIS";
    }
}
