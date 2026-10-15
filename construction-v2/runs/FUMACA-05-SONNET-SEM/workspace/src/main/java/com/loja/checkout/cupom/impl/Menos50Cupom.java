package com.loja.checkout.cupom.impl;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.domain.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50Cupom implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(PedidoContext pedido) {
        return pedido.getSubtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal frete) {
        return DESCONTO;
    }
}
