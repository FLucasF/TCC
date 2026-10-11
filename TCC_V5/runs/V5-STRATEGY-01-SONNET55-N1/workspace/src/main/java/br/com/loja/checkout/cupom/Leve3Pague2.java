package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Item;
import br.com.loja.checkout.Pedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        BigDecimal desconto = pedido.itens().stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(gratis(item))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(desconto);
    }

    private static int gratis(Item item) {
        return item.quantidade() / 3;
    }
}
