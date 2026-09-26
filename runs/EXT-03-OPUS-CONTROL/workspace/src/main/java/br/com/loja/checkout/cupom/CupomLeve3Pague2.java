package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    /** So vale se o carrinho tiver pelo menos 3 unidades de algum item. */
    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.pedido().itens().stream().anyMatch(item -> unidadesGratis(item) > 0);
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.pedido().itens().stream()
                .map(item -> item.precoUnitario().multiply(
                        BigDecimal.valueOf(unidadesGratis(item)), Dinheiro.PRECISAO))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private int unidadesGratis(Item item) {
        return item.quantidade() / 3;
    }
}
