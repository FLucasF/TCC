package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.dto.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_LEVA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(ContextoDesconto contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoDesconto contexto) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_LEVA;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return Dinheiro.arredondar(total);
    }
}
