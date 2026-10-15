package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente retira na loja: gratis, 1 dia. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias(Pedido pedido) {
        return 1;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
