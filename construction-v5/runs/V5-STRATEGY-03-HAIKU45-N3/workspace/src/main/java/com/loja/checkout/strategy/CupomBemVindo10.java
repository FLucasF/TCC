package com.loja.checkout.strategy;

import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import java.util.List;

public class CupomBemVindo10 implements CupomStrategy {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
        return subtotal.multiply(new BigDecimal("0.10"));
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal) {
        return true;
    }
}
