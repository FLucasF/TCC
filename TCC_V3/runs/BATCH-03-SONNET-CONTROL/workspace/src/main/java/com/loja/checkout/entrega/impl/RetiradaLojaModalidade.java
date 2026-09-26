package com.loja.checkout.entrega.impl;

import com.loja.checkout.domain.PedidoContexto;
import com.loja.checkout.entrega.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaModalidade implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivelPara(PedidoContexto pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContexto pedido) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
