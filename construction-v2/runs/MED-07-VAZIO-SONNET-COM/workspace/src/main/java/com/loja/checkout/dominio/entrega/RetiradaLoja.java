package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }
}
