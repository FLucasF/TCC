package com.loja.checkout.cupom;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BemVindo10 implements Cupom {
    private static final String CODIGO = "BEMVINDO10";
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return CODIGO;
    }

    @Override
    public boolean podeAplicar(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete) {
        return pedido.getSubtotalProdutos()
                .multiply(PERCENTUAL)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
