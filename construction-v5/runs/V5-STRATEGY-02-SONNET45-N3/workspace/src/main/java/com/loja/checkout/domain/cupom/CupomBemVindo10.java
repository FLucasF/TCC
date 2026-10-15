package com.loja.checkout.domain.cupom;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class CupomBemVindo10 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = contexto.subtotalProdutos().multiply(new BigDecimal("0.10"));
        return Arredondamento.arredondar(desconto);
    }

    @Override
    public boolean eAplicavel(ContextoCupom contexto) {
        return true;
    }
}
