package com.loja.checkout.entrega;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return VALOR;
    }

    @Override
    public int getPrazoDias() {
        return 0;
    }

    @Override
    public boolean isDisponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }
}
