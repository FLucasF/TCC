package loja.entrega;

import org.springframework.stereotype.Component;

import loja.dominio.Dinheiro;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean temFrete() {
        return false;
    }

    @Override
    public Cotacao cotar(ContextoEntrega ctx) {
        return new Cotacao(Dinheiro.ZERO, 1);
    }
}
