package com.loja.dominio.cupom;

import com.loja.dominio.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal MIN = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) {
        return subtotal.compareTo(MIN) >= 0;
    }

    @Override public BigDecimal desconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return DESCONTO;
    }
}
