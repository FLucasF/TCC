package br.loja.checkout.cupom;

import br.loja.checkout.Dinheiro;
import br.loja.checkout.ItemCarrinho;
import br.loja.checkout.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {
    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return pedido.itens().stream()
                .map(this::descontoDoItem)
                .reduce(Dinheiro.valor("0"), BigDecimal::add);
    }

    private BigDecimal descontoDoItem(ItemCarrinho item) {
        return Dinheiro.arredondar(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)));
    }
}
