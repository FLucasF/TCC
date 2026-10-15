package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/**
 * O cliente nao paga o frete: no resumo o frete aparece normalmente e o desconto
 * do cupom fica igual ao valor do frete.
 */
public final class FreteGratis implements Cupom {

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.emCentavos(contexto.frete());
    }
}
