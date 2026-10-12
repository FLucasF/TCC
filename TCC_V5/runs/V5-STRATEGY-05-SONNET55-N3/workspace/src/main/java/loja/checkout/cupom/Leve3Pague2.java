package loja.checkout.cupom;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;
import loja.checkout.Item;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        BigDecimal desconto = carrinho.itens().stream()
                .map(i -> i.precoUnitario().multiply(BigDecimal.valueOf(i.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(desconto);
    }
}
