package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PRECO = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_PESO_KG = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(LIMITE_PESO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return PRECO;
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }
}
