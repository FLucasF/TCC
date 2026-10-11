package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.ItemCarrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2Cupom implements Cupom {

    private static final int TAMANHO_LOTE = 3;

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
        for (ItemCarrinho item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_LOTE;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return Dinheiro.arredondar(desconto);
    }
}
