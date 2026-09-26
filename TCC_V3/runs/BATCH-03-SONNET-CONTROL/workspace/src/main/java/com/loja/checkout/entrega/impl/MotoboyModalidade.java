package com.loja.checkout.entrega.impl;

import com.loja.checkout.domain.PedidoContexto;
import com.loja.checkout.entrega.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyModalidade implements ModalidadeEntrega {

    private static final BigDecimal PRECO_FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivelPara(PedidoContexto pedido) {
        return pedido.pesoTotalKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContexto pedido) {
        return PRECO_FIXO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
