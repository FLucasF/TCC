package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PRECO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return PRECO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean atende(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO) <= 0;
    }
}
