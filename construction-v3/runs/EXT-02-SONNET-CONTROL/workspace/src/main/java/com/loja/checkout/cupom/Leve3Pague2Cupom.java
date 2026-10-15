package com.loja.checkout.cupom;

import com.loja.checkout.domain.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Cupom implements Cupom {

    private static final int TAMANHO_LEVA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_LEVA;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return desconto;
    }
}
