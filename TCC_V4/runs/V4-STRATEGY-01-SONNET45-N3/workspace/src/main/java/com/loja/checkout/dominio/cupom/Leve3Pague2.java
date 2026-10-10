package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ContextoPedido;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Leve3Pague2 implements Cupom {

    @Override
    public boolean podeAplicar(ContextoPedido contexto, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoPedido contexto, BigDecimal frete) {
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
