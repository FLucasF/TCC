package br.com.loja.checkout.entrega;

import br.com.loja.checkout.resumo.Carrinho;
import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }
}
