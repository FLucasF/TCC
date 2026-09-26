package com.loja.checkout.delivery;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Economica implements DeliveryMethod {

    private static final BigDecimal CUSTO_FIXO = new BigDecimal("12.00");
    private static final BigDecimal CUSTO_POR_KG = new BigDecimal("2.00");

    @Override
    public String getCodigo() {
        return "ECONOMICA";
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularCusto(BigDecimal pesoTotalKg) {
        return Dinheiro.arredondar(CUSTO_FIXO.add(CUSTO_POR_KG.multiply(pesoTotalKg)));
    }

    @Override
    public int getPrazoDias() {
        return 7;
    }
}
