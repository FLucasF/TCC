package com.loja.checkout.strategy;

import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements CupomStrategy {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }
}
