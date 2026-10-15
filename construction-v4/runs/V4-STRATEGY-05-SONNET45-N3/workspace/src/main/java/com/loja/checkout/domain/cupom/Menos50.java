package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.DadosCalculo;
import java.math.BigDecimal;

public class Menos50 implements Cupom {

    @Override
    public boolean podeAplicar(DadosCalculo dados) {
        return dados.getSubtotalProdutos().compareTo(new BigDecimal("300.00")) >= 0;
    }

    @Override
    public void aplicarDesconto(DadosCalculo dados) {
        dados.setDescontoCupom(new BigDecimal("50.00"));
    }
}
