package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import com.loja.checkout.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomLeve3Pague2 implements Cupom {
    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.arredondar(carrinho.itens().stream()
                .map(this::descontoDoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal descontoDoItem(Item item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
    }
}
