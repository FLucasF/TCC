package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_GRATUIDADE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / UNIDADES_POR_GRATUIDADE;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return total;
    }
}
