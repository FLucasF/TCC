package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

public final class FreteGratis implements Cupom {
    public boolean aplicavel(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return true;
    }

    public BigDecimal desconto(List<Item> itens, BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }
}
