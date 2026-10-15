package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() { return "MENOS50"; }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return subtotal.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return DESCONTO;
    }
}
