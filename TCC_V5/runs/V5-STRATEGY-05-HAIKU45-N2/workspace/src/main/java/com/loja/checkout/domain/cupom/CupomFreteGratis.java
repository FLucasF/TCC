package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.domain.Item;

public class CupomFreteGratis implements EstrategiaCupom {
    @Override
    public boolean aplicavel(BigDecimal subtotal, List<Item> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens) {
        return BigDecimal.ZERO;
    }

    @Override
    public int descricao() {
        return 0;
    }
}
