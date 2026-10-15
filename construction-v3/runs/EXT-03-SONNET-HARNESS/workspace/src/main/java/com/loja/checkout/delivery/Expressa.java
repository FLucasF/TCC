package com.loja.checkout.delivery;

import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Expressa implements DeliveryMethod {

    private static final BigDecimal CUSTO_FIXO = new BigDecimal("25.00");
    private static final BigDecimal CUSTO_POR_KG = new BigDecimal("4.50");

    @Override
    public String getCodigo() {
        return "EXPRESSA";
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
        return 2;
    }
}
