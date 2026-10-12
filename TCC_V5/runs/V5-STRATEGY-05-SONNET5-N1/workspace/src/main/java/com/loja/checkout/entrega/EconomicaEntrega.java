package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EconomicaEntrega implements EntregaCalculadora {

    private static final BigDecimal CUSTO_BASE = new BigDecimal("12.00");
    private static final BigDecimal CUSTO_POR_KG = new BigDecimal("2.00");

    @Override
    public ModalidadeEntrega getTipo() {
        return ModalidadeEntrega.ECONOMICA;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularCustoBruto(BigDecimal pesoTotalKg) {
        return CUSTO_BASE.add(CUSTO_POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }
}
