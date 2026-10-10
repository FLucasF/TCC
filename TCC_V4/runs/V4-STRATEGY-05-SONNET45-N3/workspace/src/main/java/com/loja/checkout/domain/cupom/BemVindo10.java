package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.DadosCalculo;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class BemVindo10 implements Cupom {

    @Override
    public boolean podeAplicar(DadosCalculo dados) {
        return true;
    }

    @Override
    public void aplicarDesconto(DadosCalculo dados) {
        BigDecimal desconto = dados.getSubtotalProdutos()
            .multiply(new BigDecimal("0.10"))
            .setScale(2, RoundingMode.HALF_EVEN);
        dados.setDescontoCupom(desconto);
    }
}
