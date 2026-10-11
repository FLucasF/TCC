package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class RetiradaLoja implements ModalidadeEntrega {

    public String codigo() {
        return "RETIRADA_LOJA";
    }

    public boolean atende(Carrinho carrinho) {
        return true;
    }

    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }

    public int prazoDias() {
        return 1;
    }
}
