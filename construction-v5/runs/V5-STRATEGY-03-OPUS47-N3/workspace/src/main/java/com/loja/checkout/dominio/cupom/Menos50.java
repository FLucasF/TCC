package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal VALOR = new BigDecimal("50.00");

    @Override public String codigo() { return "MENOS50"; }
    @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal) {
        return subtotal.compareTo(MINIMO) >= 0;
    }
    @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return VALOR;
    }
}
