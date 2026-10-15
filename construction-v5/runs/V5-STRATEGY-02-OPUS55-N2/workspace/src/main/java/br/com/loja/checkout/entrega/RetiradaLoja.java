package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

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
    public Cotacao cotar(Carrinho carrinho) {
        return new Cotacao(Dinheiro.ZERO, 1);
    }
}
