package br.com.loja.checkout.cupom;

import br.com.loja.checkout.resumo.Carrinho;
import br.com.loja.checkout.resumo.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        BigDecimal desconto = carrinho.itens().stream()
                .map(item -> item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }
}
