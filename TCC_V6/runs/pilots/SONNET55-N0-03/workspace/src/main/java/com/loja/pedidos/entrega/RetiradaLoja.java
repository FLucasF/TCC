package com.loja.pedidos.entrega;

import org.springframework.stereotype.Component;

import com.loja.pedidos.dominio.Dinheiro;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public Cotacao cotar(PedidoEntrega pedido) {
        return new Cotacao(Dinheiro.ZERO, 1);
    }

    @Override
    public boolean temFrete() {
        return false;
    }
}
