package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.pedido.Dinheiro;
import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O cliente busca na loja: gratis, pronto em 1 dia. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoEntregaDias(Pedido pedido) {
        return 1;
    }
}
