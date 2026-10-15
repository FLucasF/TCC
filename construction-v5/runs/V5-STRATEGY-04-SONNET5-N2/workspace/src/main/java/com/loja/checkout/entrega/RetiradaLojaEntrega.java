package com.loja.checkout.entrega;

import com.loja.checkout.pedido.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaEntrega implements MetodoEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean disponivel(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
