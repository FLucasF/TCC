package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(Pedido pedido) {
        return true;
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
