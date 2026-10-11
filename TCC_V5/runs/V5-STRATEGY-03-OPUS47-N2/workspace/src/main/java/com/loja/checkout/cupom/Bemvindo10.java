package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public class Bemvindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public void validar(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {}

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {
        return Dinheiro.arredonda(subtotal.multiply(PERCENTUAL));
    }
}
