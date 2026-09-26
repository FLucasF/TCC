package com.loja.cupom;

import java.math.BigDecimal;
import com.loja.model.Item;
import java.util.List;

public interface Cupom {
    boolean isApplicable(List<Item> itens, BigDecimal subtotal);
    BigDecimal calculateDiscount(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
    String getCode();
}
