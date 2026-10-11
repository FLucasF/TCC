package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");

    @Override
    public String getCodigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return TAXA_FIXA;
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
