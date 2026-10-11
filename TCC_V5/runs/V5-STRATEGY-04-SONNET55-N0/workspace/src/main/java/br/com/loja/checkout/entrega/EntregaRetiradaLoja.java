package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class EntregaRetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
