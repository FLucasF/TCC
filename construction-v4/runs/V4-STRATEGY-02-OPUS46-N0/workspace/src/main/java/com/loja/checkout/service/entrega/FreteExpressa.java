package com.loja.checkout.service.entrega;

import com.loja.checkout.service.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteExpressa implements CalculadoraFrete {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public String codigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return Moeda.arredondar(TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotal)));
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }
}
