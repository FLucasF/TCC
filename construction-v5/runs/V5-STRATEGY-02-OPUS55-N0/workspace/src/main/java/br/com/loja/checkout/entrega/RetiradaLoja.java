package br.com.loja.checkout.entrega;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calcularFrete(Carrinho carrinho) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
