package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_PARA_GANHAR_UMA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom pedido) {
        BigDecimal desconto = pedido.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }

    private BigDecimal unidadesGratis(Item item) {
        int gratis = item.quantidade() / UNIDADES_PARA_GANHAR_UMA;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
