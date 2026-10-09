package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

/** Gratis, pronta para retirada em 1 dia. */
@Component
class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return true;
    }

    @Override
    public Entrega calcular(Carrinho carrinho) {
        return new Entrega(Dinheiro.ZERO, 1);
    }
}
