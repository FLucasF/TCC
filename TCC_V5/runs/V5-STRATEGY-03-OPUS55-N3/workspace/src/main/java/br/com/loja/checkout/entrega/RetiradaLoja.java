package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Dinheiro;
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
        return Dinheiro.centavos(BigDecimal.ZERO);
    }
}
