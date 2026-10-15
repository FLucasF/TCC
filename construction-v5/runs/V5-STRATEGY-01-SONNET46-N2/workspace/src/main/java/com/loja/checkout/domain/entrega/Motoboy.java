package com.loja.checkout.domain.entrega;

import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CodigoErro;

import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal FRETE_FIXO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5");

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return FRETE_FIXO.setScale(2);
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public void validarDisponibilidade(BigDecimal pesoTotal) {
        if (pesoTotal.compareTo(PESO_MAXIMO) > 0) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
    }
}
