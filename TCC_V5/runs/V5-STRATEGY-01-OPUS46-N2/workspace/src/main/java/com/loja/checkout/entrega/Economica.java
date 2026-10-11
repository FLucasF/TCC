package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class Economica implements ModalidadeEntrega {

    private static final BigDecimal FIXO = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return FIXO.add(POR_KG.multiply(pesoTotal)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
