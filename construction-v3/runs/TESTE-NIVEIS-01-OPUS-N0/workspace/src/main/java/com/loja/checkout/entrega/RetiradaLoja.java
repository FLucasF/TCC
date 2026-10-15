package com.loja.checkout.entrega;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.pedido.Pedido;
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
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.ZERO;
    }
}
