package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.DadosCalculo;

public class FreteGratis implements Cupom {

    @Override
    public boolean podeAplicar(DadosCalculo dados) {
        return true;
    }

    @Override
    public void aplicarDesconto(DadosCalculo dados) {
        dados.setDescontoCupom(dados.getFrete());
    }
}
