package com.loja.checkout.cupom;

import com.loja.checkout.calculo.Dinheiro;
import com.loja.checkout.calculo.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Bemvindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public boolean aplicavel(BigDecimal subtotal, BigDecimal frete, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<ItemCarrinho> itens) {
        return Dinheiro.cents(subtotal.multiply(PERCENTUAL));
    }
}
