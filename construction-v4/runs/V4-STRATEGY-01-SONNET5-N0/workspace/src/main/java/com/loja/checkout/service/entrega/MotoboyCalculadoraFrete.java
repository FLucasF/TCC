package com.loja.checkout.service.entrega;

import com.loja.checkout.enums.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyCalculadoraFrete implements CalculadoraFrete {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public ModalidadeEntrega getModalidade() {
        return ModalidadeEntrega.MOTOBOY;
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
