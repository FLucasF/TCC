package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Motoboy implements ModalidadeEntrega {

    private static final double PESO_MAXIMO_KG = 5.0;
    private static final BigDecimal VALOR = BigDecimal.valueOf(18.00);

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean disponivel(double pesoTotalKg) {
        return pesoTotalKg <= PESO_MAXIMO_KG;
    }

    @Override
    public BigDecimal calcularFrete(double pesoTotalKg) {
        return VALOR.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }
}
