package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O cliente retira na loja: gratis, disponivel no dia seguinte. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal custo(Pedido pedido) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 1;
    }
}
