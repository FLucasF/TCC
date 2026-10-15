package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy extends EntregaLinear {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    public Motoboy() {
        super("18.00", "0.00", 0);
    }

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    /** Só leva pedidos de até 5 kg. */
    @Override
    public boolean disponivel(BigDecimal pesoKg) {
        return pesoKg.compareTo(PESO_MAXIMO) <= 0;
    }
}
