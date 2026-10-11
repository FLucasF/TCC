package com.loja.dominio.cupom;

import com.loja.dominio.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override public boolean aplicavel(List<ItemCarrinho> itens, BigDecimal subtotal) { return true; }

    @Override public BigDecimal desconto(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }
}
