package br.loja.checkout.entrega;

import br.loja.checkout.Dinheiro;
import br.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class RetiradaLoja implements Entrega {
    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.valor("0");
    }

    @Override
    public int prazoDias(Pedido pedido) {
        return 1;
    }
}
