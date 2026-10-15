package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.DadosCalculo;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Leve3Pague2 implements Cupom {

    @Override
    public boolean podeAplicar(DadosCalculo dados) {
        return true;
    }

    @Override
    public void aplicarDesconto(DadosCalculo dados) {
        BigDecimal descontoTotal = dados.getItens().stream()
            .map(item -> {
                int unidadesGratis = item.quantidade() / 3;
                return item.precoUnitario()
                    .multiply(new BigDecimal(unidadesGratis))
                    .setScale(2, RoundingMode.HALF_EVEN);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        dados.setDescontoCupom(descontoTotal);
    }
}
