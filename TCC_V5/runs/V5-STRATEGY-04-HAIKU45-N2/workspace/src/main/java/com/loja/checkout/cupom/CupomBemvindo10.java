package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;
import com.loja.checkout.util.Arredonda;

public class CupomBemvindo10 implements CalculadoraCupom {
    @Override
    public BigDecimal calcular(List<Item> itens, BigDecimal subtotal) {
        return Arredonda.arredonda(subtotal.multiply(new BigDecimal("0.10")));
    }

    @Override
    public int getPrazoEntrega() {
        return 0;
    }
}
