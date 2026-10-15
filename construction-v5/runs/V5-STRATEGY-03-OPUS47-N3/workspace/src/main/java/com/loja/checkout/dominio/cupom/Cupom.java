package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    String codigo();
    boolean aplicavel(List<Item> itens, BigDecimal subtotal);
    BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
}
