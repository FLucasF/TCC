package com.loja.checkout.entrega;

import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Gratis, 1 dia. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
