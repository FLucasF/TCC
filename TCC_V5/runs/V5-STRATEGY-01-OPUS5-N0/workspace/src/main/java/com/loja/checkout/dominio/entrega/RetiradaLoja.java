package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Gratis, pronta para retirada em 1 dia. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKgPedido) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
