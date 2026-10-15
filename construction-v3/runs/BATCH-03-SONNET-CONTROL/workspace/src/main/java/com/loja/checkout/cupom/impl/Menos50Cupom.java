package com.loja.checkout.cupom.impl;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.domain.PedidoContexto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50Cupom implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(PedidoContexto pedido, BigDecimal frete) {
        return pedido.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContexto pedido, BigDecimal frete) {
        return DESCONTO;
    }
}
