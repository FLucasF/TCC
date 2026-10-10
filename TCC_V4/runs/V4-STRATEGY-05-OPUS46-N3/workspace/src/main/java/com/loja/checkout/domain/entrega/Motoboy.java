package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal CUSTO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return CUSTO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }
}
