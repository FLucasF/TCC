package com.loja.checkout.strategy;

import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import java.util.List;

public interface CupomStrategy {
    BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens);
    boolean isAplicavel(BigDecimal subtotal);
}
