package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Gratis; prazo 1 dia; sem limitacao. */
@Component
public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public String codigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(BigDecimal.ZERO);
    }

    @Override
    public int prazoDias() {
        return 1;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoKg) {
        // Sempre disponivel.
    }
}
