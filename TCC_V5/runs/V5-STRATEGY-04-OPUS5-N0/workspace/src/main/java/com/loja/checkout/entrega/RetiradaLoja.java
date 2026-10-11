package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** O cliente busca na loja: grátis, disponível no dia seguinte. */
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
    public int prazoEntregaDias() {
        return 1;
    }
}
