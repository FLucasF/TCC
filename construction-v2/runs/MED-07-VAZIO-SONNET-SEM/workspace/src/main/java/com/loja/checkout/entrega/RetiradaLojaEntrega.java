package com.loja.checkout.entrega;

import com.loja.checkout.dominio.PedidoContext;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLojaEntrega implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("0.00");
    private static final int PRAZO_DIAS = 1;

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(PedidoContext pedido) {
        return true;
    }

    @Override
    public CalculoFrete calcular(PedidoContext pedido) {
        return new CalculoFrete(VALOR, PRAZO_DIAS);
    }
}
