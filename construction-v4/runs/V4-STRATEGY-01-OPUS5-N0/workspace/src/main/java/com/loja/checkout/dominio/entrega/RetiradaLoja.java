package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Moeda;
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
    public int prazoDias() {
        return 1;
    }

    @Override
    public BigDecimal calcularFrete(DadosEntrega dados) {
        return Moeda.ZERO;
    }
}
