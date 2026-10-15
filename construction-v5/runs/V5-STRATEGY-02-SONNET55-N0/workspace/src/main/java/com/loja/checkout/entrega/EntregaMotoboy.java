package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class EntregaMotoboy implements OpcaoEntrega {
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");
    private static final BigDecimal VALOR = new BigDecimal("18.00");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredondar(VALOR);
    }
}
