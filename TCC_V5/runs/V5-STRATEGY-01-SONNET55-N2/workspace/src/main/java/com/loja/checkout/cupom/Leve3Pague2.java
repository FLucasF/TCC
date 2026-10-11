package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavelA(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : carrinho.itens()) {
            BigDecimal gratis = BigDecimal.valueOf(item.quantidade() / 3);
            desconto = desconto.add(item.precoUnitario().multiply(gratis));
        }
        return Dinheiro.arredondar(desconto);
    }
}
