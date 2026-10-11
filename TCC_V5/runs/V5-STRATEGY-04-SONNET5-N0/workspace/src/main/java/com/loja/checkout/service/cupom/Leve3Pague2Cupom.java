package com.loja.checkout.service.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2Cupom implements Cupom {

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean isAplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return Money.round(desconto);
    }
}
