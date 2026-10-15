package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("MOTOBOY")
public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotalKg) {
        return TAXA_FIXA;
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotalKg) {
        if (pesoTotalKg.compareTo(PESO_MAXIMO_KG) > 0) {
            indisponivel();
        }
    }
}
