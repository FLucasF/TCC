package com.loja.checkout.entrega.impl;

import com.loja.checkout.domain.PedidoContext;
import com.loja.checkout.entrega.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyEntrega implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(PedidoContext pedido) {
        return pedido.getPesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContext pedido) {
        return TAXA_FIXA;
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }
}
