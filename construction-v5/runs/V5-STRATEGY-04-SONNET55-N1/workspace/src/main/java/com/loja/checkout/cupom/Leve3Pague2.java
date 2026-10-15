package com.loja.checkout.cupom;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {
    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(ContextoCupom pedido) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom pedido) {
        return pedido.itens().stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade() / 3)))
                .map(Dinheiro::arredondar)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }
}
