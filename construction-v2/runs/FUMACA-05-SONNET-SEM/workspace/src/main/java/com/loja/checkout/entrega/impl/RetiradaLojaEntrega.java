package com.loja.checkout.entrega.impl;

import com.loja.checkout.domain.PedidoContext;
import com.loja.checkout.entrega.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaEntrega implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContext pedido) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }
}
