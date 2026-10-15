package com.loja.checkout.strategy;

import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements CupomStrategy {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete, List<Item> itens) {
        return frete;
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal) {
        return true;
    }
}
