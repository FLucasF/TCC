package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
@Component
class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return contexto.pedido().itens().stream()
                .map(item -> Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
