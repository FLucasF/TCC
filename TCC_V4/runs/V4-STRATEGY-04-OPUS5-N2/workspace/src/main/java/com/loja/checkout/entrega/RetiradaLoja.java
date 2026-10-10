package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** O cliente busca na loja: gratis, em 1 dia. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return Dinheiro.ZERO;
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
