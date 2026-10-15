package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / 3;
            if (unidadesGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
            }
        }
        return Dinheiro.arredondar(desconto);
    }
}
