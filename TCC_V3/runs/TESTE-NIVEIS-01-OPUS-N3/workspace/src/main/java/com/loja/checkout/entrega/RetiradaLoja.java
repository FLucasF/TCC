package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

/** Grátis, pronto para retirar em 1 dia. */
@Component
class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public Entrega apurar(Pedido pedido) {
        return new Entrega(Dinheiro.ZERO, 1);
    }
}
