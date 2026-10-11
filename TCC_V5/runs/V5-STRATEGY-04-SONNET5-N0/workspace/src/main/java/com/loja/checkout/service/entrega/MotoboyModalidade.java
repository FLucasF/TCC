package com.loja.checkout.service.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MotoboyModalidade implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA;
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
