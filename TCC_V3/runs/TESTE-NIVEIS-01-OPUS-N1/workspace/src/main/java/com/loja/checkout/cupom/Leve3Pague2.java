package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(BaseCupom base) {
        return true;
    }

    @Override
    public BigDecimal desconto(BaseCupom base) {
        BigDecimal desconto = base.carrinho().itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }

    private BigDecimal unidadesGratis(Item item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
    }
}
