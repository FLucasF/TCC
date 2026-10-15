package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.DadosCalculo;

public interface Cupom {
    boolean podeAplicar(DadosCalculo dados);
    void aplicarDesconto(DadosCalculo dados);
}
