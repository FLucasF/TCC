package com.loja.checkout.cupom;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public class FreteGratis implements Cupom {
    private static final String CODIGO = "FRETEGRATIS";

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
        return frete;
    }
}
