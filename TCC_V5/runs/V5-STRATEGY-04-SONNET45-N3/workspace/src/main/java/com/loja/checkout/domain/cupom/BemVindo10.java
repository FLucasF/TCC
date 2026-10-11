package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ContextoCalculo;
import com.loja.checkout.domain.Arredondamento;
import java.math.BigDecimal;

public class BemVindo10 implements Cupom {
    @Override
    public boolean aplicavel(ContextoCalculo contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCalculo contexto) {
        BigDecimal desconto = contexto.getSubtotalProdutos()
            .multiply(new BigDecimal("0.10"));
        return Arredondamento.arredondar(desconto);
    }
}
