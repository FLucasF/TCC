package com.loja.dominio.cupom;

import com.loja.dominio.Dinheiro;
import com.loja.dominio.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class Bemvindo10 implements Cupom {
    private static final BigDecimal TAXA = new BigDecimal("0.10");

    @Override public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) { return true; }

    @Override public BigDecimal desconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return Dinheiro.centavos(subtotal.multiply(TAXA));
    }
}
