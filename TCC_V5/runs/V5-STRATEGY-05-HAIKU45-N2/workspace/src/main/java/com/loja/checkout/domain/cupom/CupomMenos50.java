package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.domain.Item;

public class CupomMenos50 implements EstrategiaCupom {
    @Override
    public boolean aplicavel(BigDecimal subtotal, List<Item> itens) {
        return subtotal.compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens) {
        return new BigDecimal("50.00");
    }

    @Override
    public int descricao() {
        return 0;
    }
}
