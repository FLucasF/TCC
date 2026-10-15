package com.loja.checkout.cupom.impl;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.domain.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCupom implements Cupom {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal frete) {
        return frete;
    }
}
