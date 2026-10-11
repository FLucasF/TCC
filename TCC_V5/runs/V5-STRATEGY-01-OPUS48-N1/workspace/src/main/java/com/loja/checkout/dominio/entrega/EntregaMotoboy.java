package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaMotoboy implements PoliticaEntrega {

    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal custo(BigDecimal pesoKg) {
        return CUSTO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO) <= 0;
    }
}
