package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;

public interface CalculadoraCupom {
    BigDecimal calcular(List<Item> itens, BigDecimal subtotal);
    int getPrazoEntrega();
}
