package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.carrinho.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : contexto.carrinho().itens()) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(Dinheiro.centavos(item.precoUnitario().multiply(BigDecimal.valueOf(gratis))));
        }
        return Dinheiro.centavos(desconto);
    }
}
