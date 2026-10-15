package com.loja.checkout.dominio.entrega;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Cliente retira na loja: gratis, em 1 dia. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
