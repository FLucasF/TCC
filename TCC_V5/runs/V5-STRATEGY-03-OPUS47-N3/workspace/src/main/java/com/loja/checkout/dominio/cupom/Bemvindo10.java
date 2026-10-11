package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public class Bemvindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override public String codigo() { return "BEMVINDO10"; }
    @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal) { return true; }
    @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return Dinheiro.arredondar(subtotal.multiply(PERCENTUAL));
    }
}
