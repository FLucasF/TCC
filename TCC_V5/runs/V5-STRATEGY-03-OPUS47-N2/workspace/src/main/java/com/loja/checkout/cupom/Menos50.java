package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.ErroPedido;
import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public class Menos50 implements Cupom {
    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public void validar(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {
        if (subtotal.compareTo(MINIMO) < 0) {
            throw new ErroPedido("CUPOM_NAO_APLICAVEL");
        }
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {
        return Dinheiro.arredonda(DESCONTO);
    }
}
