package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

/** O cliente busca na loja: nao paga frete e fica pronto em 1 dia. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    private static final int PRAZO_DIAS = 1;

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public Entrega calcular(Pedido pedido) {
        return new Entrega(Dinheiro.ZERO, PRAZO_DIAS);
    }
}
