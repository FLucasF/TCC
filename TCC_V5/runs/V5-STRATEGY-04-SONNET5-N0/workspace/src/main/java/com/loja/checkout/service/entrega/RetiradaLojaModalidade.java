package com.loja.checkout.service.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLojaModalidade implements ModalidadeEntrega {

    @Override
    public String getCodigo() {
        return "RETIRADA_LOJA";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotalKg) {
        return true;
    }
}
