package com.loja.checkout.cupom;

import com.loja.checkout.calculo.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean aplicavel(BigDecimal subtotal, BigDecimal frete, List<ItemCarrinho> itens);
    BigDecimal desconto(BigDecimal subtotal, BigDecimal frete, List<ItemCarrinho> itens);
}
