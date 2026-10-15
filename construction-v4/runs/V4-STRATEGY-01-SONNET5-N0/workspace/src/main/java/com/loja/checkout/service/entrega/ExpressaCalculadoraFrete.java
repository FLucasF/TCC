package com.loja.checkout.service.entrega;

import com.loja.checkout.enums.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpressaCalculadoraFrete implements CalculadoraFrete {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("25.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("4.50");

    @Override
    public ModalidadeEntrega getModalidade() {
        return ModalidadeEntrega.EXPRESSA;
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA.add(TAXA_POR_KG.multiply(pesoTotalKg));
    }

    @Override
    public int prazoDias() {
        return 2;
    }
}
