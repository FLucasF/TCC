package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemPedido;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class CupomLeve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemPedido item : contexto.itens()) {
            int quantidade = item.quantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.precoUnitario()
                    .multiply(BigDecimal.valueOf(unidadesGratis));
                descontoTotal = descontoTotal.add(descontoItem);
            }
        }

        return Arredondamento.arredondar(descontoTotal);
    }

    @Override
    public boolean eAplicavel(ContextoCupom contexto) {
        return true;
    }
}
