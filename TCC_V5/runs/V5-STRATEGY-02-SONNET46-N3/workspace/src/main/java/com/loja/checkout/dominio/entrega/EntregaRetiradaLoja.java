package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

public class EntregaRetiradaLoja implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKgTotal) {
        return new BigDecimal("0.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoKgTotal) {
        return true;
    }
}
