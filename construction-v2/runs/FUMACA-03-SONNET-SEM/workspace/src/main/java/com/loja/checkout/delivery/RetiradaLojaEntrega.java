package com.loja.checkout.delivery;

import com.loja.checkout.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaEntrega implements ModalidadeEntregaStrategy {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean disponivelPara(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContext pedido) {
        return BigDecimal.ZERO;
    }
}
