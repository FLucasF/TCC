package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** R$ 12,00 + R$ 2,00 por kg; prazo 7 dias; sem limitacao. */
@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(FIXO.add(POR_KG.multiply(pesoKg)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoKg) {
        // Sempre disponivel.
    }
}
