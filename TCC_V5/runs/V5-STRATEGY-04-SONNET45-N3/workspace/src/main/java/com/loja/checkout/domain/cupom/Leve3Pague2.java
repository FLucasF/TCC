package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ContextoCalculo;
import com.loja.checkout.domain.Arredondamento;
import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;

public class Leve3Pague2 implements Cupom {
    @Override
    public boolean aplicavel(ContextoCalculo contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCalculo contexto) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : contexto.getItens()) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(unidadesGratis));
            descontoTotal = descontoTotal.add(descontoItem);
        }

        return Arredondamento.arredondar(descontoTotal);
    }
}
