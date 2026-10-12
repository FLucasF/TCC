package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import com.loja.checkout.pedido.Item;

public interface Cupom {

    String codigo();

    BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
}
