package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemCarrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return contexto.carrinho().itens().stream()
                .map(this::descontoDoItem)
                .reduce(Dinheiro.ZERO, BigDecimal::add);
    }

    private BigDecimal descontoDoItem(ItemCarrinho item) {
        int gratis = item.quantidade() / 3;
        return Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
    }
}
