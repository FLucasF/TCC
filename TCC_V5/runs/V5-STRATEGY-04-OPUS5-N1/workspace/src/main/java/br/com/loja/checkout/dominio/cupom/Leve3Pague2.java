package br.com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;

final class Leve3Pague2 implements Cupom {

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
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : contexto.pedido().itens()) {
            BigDecimal gratuitas = BigDecimal.valueOf(item.quantidade() / 3);
            desconto = desconto.add(item.precoUnitario().multiply(gratuitas));
        }
        return Dinheiro.centavos(desconto);
    }
}
