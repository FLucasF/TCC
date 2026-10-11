package com.loja.checkout.cupom;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public class Menos50 implements Cupom {
    private static final String CODIGO = "MENOS50";
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");

    @Override
    public String getCodigo() {
        return CODIGO;
    }

    @Override
    public boolean podeAplicar(Pedido pedido, BigDecimal frete) {
        return pedido.getSubtotalProdutos().compareTo(VALOR_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        return DESCONTO;
    }
}
