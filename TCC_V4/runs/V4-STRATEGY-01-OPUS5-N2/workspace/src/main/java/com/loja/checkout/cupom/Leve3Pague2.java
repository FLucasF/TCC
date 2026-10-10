package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_BRINDE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.arredonda(contexto.pedido().itens().stream()
                .map(Leve3Pague2::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal unidadesGratis(Item item) {
        int gratis = item.quantidade() / UNIDADES_POR_BRINDE;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
