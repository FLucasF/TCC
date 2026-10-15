package com.loja.checkout.entrega;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoKg) {
        return true;
    }
}
