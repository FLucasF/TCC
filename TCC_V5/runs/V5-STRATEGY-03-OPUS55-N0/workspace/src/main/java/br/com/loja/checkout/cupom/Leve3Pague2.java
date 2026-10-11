package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
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
        for (Item item : contexto.pedido().itens()) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return Dinheiro.arredondar(desconto);
    }
}
