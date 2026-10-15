package com.loja.checkout.service.entrega;

import com.loja.checkout.service.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpressaEntrega implements EntregaCalculadora {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public String getCodigo() {
        return "EXPRESSA";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return Dinheiro.arredondar(TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotalKg)));
    }

    @Override
    public int getPrazoDias() {
        return 2;
    }
}
