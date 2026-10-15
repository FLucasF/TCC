package com.loja.resumo.cupom;

import com.loja.resumo.Contexto;
import com.loja.resumo.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Contexto contexto) {
        return contexto.itens().stream()
                .map(this::valorGratisDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valorGratisDoItem(ItemPedido item) {
        int unidadesGratis = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
    }
}
