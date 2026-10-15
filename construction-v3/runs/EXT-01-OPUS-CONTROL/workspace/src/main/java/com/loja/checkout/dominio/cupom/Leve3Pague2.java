package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        BigDecimal desconto = contexto.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Moeda.centavos(desconto);
    }

    private BigDecimal unidadesGratis(Item item) {
        int gratis = item.quantidade() / 3;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
