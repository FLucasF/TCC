package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MotoboyOpcaoEntrega implements OpcaoEntrega {

    private static final BigDecimal CUSTO_FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public ModalidadeEntrega modalidade() {
        return ModalidadeEntrega.MOTOBOY;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return CUSTO_FIXO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
