package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

@Component
public class RetiradaNaLoja implements ModalidadeEntrega {

    private static final int PRAZO_DIAS = 1;

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public Entrega calcular(Pedido pedido) {
        return new Entrega(Dinheiro.ZERO, PRAZO_DIAS);
    }
}
