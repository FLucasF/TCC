package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Motoboy: R$ 18,00, mesmo dia (0 dias). So leva pedidos de ate 5 kg. */
@Component
public class EntregaMotoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean atende(BigDecimal pesoTotalKg) {
        return pesoTotalKg.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return VALOR;
    }
}
