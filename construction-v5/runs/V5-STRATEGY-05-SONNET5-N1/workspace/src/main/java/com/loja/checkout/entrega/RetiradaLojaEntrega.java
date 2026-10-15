package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RetiradaLojaEntrega implements EntregaCalculadora {

    @Override
    public ModalidadeEntrega getTipo() {
        return ModalidadeEntrega.RETIRADA_LOJA;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularCustoBruto(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoDias() {
        return 1;
    }
}
