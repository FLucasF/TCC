package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

class BemVindo10 implements Cupom {

    @Override
    public String codigo() { return "BEMVINDO10"; }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) { return true; }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
    }
}
