package br.com.loja.checkout.cupom;

import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {

    private static final int LEVE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.arredondar(carrinho.itens().stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / LEVE)))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
