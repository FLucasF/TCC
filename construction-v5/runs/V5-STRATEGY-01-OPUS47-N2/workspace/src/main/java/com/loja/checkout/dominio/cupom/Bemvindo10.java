package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

public final class Bemvindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return true;
    }

    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return Dinheiro.arredondar(subtotal.multiply(PERCENTUAL));
    }
}
