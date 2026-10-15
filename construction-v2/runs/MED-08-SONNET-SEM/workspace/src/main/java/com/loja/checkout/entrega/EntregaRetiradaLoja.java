package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EntregaRetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean disponivelPara(PedidoContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(PedidoContexto contexto) {
        return BigDecimal.ZERO.setScale(2);
    }
}
