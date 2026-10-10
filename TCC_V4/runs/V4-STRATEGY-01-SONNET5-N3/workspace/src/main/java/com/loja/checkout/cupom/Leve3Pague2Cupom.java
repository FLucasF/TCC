package com.loja.checkout.cupom;

import com.loja.checkout.model.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("LEVE3PAGUE2")
public class Leve3Pague2Cupom implements Cupom {

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return desconto;
    }
}
