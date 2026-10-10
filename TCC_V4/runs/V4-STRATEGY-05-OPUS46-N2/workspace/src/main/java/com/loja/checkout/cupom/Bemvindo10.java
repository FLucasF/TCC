package com.loja.checkout.cupom;

import com.loja.checkout.CheckoutRequest.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Bemvindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return subtotal.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }
}
