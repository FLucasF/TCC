package com.loja.checkout.entrega.impl;

import com.loja.checkout.domain.PedidoContext;
import com.loja.checkout.entrega.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaEntrega implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean disponivel(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContext pedido) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pedido.getPesoTotalKg()));
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }
}
