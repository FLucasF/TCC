package com.loja.checkout.cupom;

import com.loja.checkout.CheckoutRequest.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }
}
