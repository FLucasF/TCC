package com.loja.checkout.cupom;

import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public class FreteGratis implements Cupom {
    @Override
    public void validar(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {}

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }
}
