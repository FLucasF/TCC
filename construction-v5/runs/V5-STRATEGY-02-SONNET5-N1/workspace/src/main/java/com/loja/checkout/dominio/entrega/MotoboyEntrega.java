package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MotoboyEntrega implements ModalidadeEntrega {

    private static final BigDecimal VALOR_FIXO = new BigDecimal("18.00");
    private static final BigDecimal LIMITE_PESO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(LIMITE_PESO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularValor(BigDecimal pesoTotalKg) {
        return VALOR_FIXO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
