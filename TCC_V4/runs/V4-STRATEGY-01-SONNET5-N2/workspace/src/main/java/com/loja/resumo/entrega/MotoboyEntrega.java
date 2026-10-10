package com.loja.resumo.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MotoboyEntrega implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivelPara(BigDecimal pesoKg) {
        return pesoKg.compareTo(LIMITE_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return VALOR;
    }
}
