package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Dinheiro;

import java.math.BigDecimal;

/** Pedido sem cupom: nunca desconta nada. Evita tratar "sem cupom" como caso especial. */
public class SemCupom implements Cupom {

    @Override
    public String codigo() {
        return null;
    }

    @Override
    public void validarAplicabilidade(DadosCupom dados) {
        // Nada a validar.
    }

    @Override
    public BigDecimal desconto(DadosCupom dados) {
        return Dinheiro.centavos(BigDecimal.ZERO);
    }
}
