package com.loja.checkout.cupom;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Dinheiro;
import com.loja.checkout.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {
    public String codigo() { return "LEVE3PAGUE2"; }

    public boolean aplicavel(Carrinho carrinho) { return true; }

    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : carrinho.itens()) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)));
        }
        return Dinheiro.arredondar(total);
    }
}
