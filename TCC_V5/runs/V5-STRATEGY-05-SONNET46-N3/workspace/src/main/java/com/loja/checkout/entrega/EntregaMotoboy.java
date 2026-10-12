package com.loja.checkout.entrega;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;

import java.math.BigDecimal;

public class EntregaMotoboy implements ModalidadeEntrega {

    private static final double PESO_MAXIMO_KG = 5.0;

    @Override
    public BigDecimal calcularFrete(double pesoKg) {
        return new BigDecimal("18.00");
    }

    @Override
    public int getPrazo() {
        return 0;
    }

    @Override
    public void validarDisponibilidade(double pesoKg) {
        if (pesoKg > PESO_MAXIMO_KG) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INDISPONIVEL);
        }
    }
}
