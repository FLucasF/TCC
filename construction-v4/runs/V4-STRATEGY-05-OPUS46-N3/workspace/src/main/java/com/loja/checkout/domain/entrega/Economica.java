package com.loja.checkout.domain.entrega;

import com.loja.checkout.domain.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return Moeda.arredondar(FIXO.add(POR_KG.multiply(pesoTotal)));
    }

    @Override
    public int prazoDias() {
        return 7;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }
}
