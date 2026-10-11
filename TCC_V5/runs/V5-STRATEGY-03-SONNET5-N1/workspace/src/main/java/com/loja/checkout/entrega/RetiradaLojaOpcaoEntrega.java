package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class RetiradaLojaOpcaoEntrega implements OpcaoEntrega {

    @Override
    public ModalidadeEntrega modalidade() {
        return ModalidadeEntrega.RETIRADA_LOJA;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoDias() {
        return 1;
    }
}
