package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;

public class CupomMenos50 implements CalculadoraCupom {
    @Override
    public BigDecimal calcular(List<Item> itens, BigDecimal subtotal) {
        return new BigDecimal("50.00");
    }

    @Override
    public int getPrazoEntrega() {
        return 0;
    }

    public boolean podeAplicar(BigDecimal subtotal) {
        return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }
}
