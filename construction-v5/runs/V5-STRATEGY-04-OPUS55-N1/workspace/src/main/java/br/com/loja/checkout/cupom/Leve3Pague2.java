package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.carrinho.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Leve3Pague2 implements Cupom {

    private static final int A_CADA = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.carrinho().itens().stream()
                .map(Leve3Pague2::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal unidadesGratis(Item item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / A_CADA));
    }
}
