package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Item;
import loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

@Component
class Leve3Pague2 implements Cupom {
    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return pedido.itens().stream().anyMatch(i -> i.quantidade() >= 3);
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : pedido.itens()) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)));
        }
        return Dinheiro.arredondar(total);
    }
}
