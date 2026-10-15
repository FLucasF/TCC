package com.loja.checkout.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("MOTOBOY")
public class MotoboyModalidade implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");
    private static final BigDecimal VALOR_FIXO = new BigDecimal("18.00");

    @Override
    public boolean disponivelPara(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return VALOR_FIXO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
