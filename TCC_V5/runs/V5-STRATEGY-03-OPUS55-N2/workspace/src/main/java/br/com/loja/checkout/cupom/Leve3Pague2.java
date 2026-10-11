package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {

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
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
    }
}
