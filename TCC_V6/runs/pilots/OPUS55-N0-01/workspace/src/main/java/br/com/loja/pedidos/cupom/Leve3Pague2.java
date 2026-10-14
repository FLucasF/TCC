package br.com.loja.pedidos.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.checkout.ItemCarrinho;
import br.com.loja.pedidos.comum.Dinheiro;

/** A cada 3 unidades de um mesmo produto, uma sai de graça. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        BigDecimal desconto = Dinheiro.ZERO;
        for (ItemCarrinho item : contexto.itens()) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.produto().preco().multiply(BigDecimal.valueOf(gratis)));
        }
        if (desconto.signum() == 0) {
            throw Cupom.naoAplicavel();
        }
        return Dinheiro.centavos(desconto);
    }
}
