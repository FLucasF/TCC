package com.loja.checkout.cupom;

import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public interface Cupom {
    void validar(Pedido pedido, BigDecimal subtotal, BigDecimal frete);
    BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotal, BigDecimal frete);
}
