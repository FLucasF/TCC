package com.loja.checkout.service.cupom;

import com.loja.checkout.service.Dinheiro;
import com.loja.checkout.web.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Cupom implements CupomStrategy {

    @Override
    public String getCodigo() {
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
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return Dinheiro.arredondar(desconto);
    }
}
