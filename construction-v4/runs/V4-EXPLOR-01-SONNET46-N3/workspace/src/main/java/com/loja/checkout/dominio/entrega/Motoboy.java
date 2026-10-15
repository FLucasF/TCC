package com.loja.checkout.dominio.entrega;

import com.loja.checkout.infra.CheckoutException;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoKg) {
        if (pesoKg.compareTo(PESO_MAXIMO_KG) > 0) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
    }
}
