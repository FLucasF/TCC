package br.com.loja.checkout.cupom;

import br.com.loja.checkout.resumo.Carrinho;
import br.com.loja.checkout.resumo.Dinheiro;
import br.com.loja.checkout.resumo.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.centavos(carrinho.itens().stream()
                .map(Leve3Pague2::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal unidadesGratis(Item item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
    }
}
