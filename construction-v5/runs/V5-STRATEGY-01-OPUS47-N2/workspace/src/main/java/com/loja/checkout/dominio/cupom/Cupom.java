package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
    BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete);
}
