package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MotoboyEntrega implements EntregaCalculadora {

    private static final BigDecimal CUSTO_FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public ModalidadeEntrega getTipo() {
        return ModalidadeEntrega.MOTOBOY;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularCustoBruto(BigDecimal pesoTotalKg) {
        return CUSTO_FIXO;
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }
}
