package br.com.loja.pedidos.entrega;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.comum.Dinheiro;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public CotacaoEntrega cotar(ContextoEntrega contexto) {
        return new CotacaoEntrega(Dinheiro.ZERO, 1);
    }

    @Override
    public boolean temFrete() {
        return false;
    }
}
