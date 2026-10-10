package com.loja.checkout.cupom;

import com.loja.checkout.CheckoutRequest.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return DESCONTO;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return subtotal.compareTo(MINIMO) >= 0;
    }
}
