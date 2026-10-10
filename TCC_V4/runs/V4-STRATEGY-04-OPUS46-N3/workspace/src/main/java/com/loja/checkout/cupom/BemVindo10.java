package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return subtotal.multiply(PERCENTUAL).setScale(2, RoundingMode.HALF_EVEN);
    }
}
