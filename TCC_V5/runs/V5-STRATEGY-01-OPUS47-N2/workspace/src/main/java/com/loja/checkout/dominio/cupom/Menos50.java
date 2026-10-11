package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

public final class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return subtotal.compareTo(MINIMO) >= 0;
    }

    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return DESCONTO;
    }
}
