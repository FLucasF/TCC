package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override public String codigo() { return "FRETEGRATIS"; }
    @Override public boolean aplicavel(List<Item> itens, BigDecimal subtotal) { return true; }
    @Override public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }
}
