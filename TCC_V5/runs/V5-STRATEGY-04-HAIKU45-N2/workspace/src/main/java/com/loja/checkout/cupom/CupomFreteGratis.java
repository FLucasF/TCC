package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;
import com.loja.checkout.util.Arredonda;

public class CupomFreteGratis implements CalculadoraCupom {
    @Override
    public BigDecimal calcular(List<Item> itens, BigDecimal subtotal) {
        return Arredonda.arredonda(BigDecimal.ZERO);
    }

    @Override
    public int getPrazoEntrega() {
        return 0;
    }
}
