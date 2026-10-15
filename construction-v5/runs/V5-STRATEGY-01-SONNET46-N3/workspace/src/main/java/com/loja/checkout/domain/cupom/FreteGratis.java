package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

class FreteGratis implements Cupom {

    @Override
    public String codigo() { return "FRETEGRATIS"; }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) { return true; }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return frete;
    }
}
